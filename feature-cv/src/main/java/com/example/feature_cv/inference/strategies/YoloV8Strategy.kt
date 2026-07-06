package com.example.feature_cv.inference.strategies

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.scale
import com.example.feature_cv.inference.ModelStrategy
import com.example.feature_cv.inference.model.LetterboxTransform
import com.example.feature_cv.inference.model.PreprocessResult
import com.example.feature_cv.inference.model.RawDetection
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Реализация ModelStrategy под YOLOv8, экспортированную в TFLite через Ultralytics `litert`.
 *
 * Проверенная конвенция (см. Python-диагностику перед переносом в Kotlin):
 * - Layout входа: NCHW, то есть [1, 3, inputSize, inputSize]
 * - Выход: [1, 4+numClasses, numAnchors] — координаты (cx,cy,w,h) НОРМАЛИЗОВАНЫ в [0,1]
 *   относительно inputSize (умножаем на inputSize перед пересчётом через letterbox)
 * - NMS должен быть РАЗДЕЛЬНЫМ по классам — иначе соседние знаки разных типов
 *   на одном столбе гасят друг друга (см. обсуждение архитектуры)
 */
class YoloV8Strategy(
    override val inputSize: Int,
    override val fileName: String,
    private val numClasses: Int = 155,
    // Color.rgb(114, 114, 114) даёт ровно то же самое значение, но не тянет Android-класс
    // в default-параметр конструктора — Color.rgb() в plain JUnit тестах (не androidTest)
    // не замокан и падает с рантайм-исключением при простом создании объекта.
    private val letterboxColor: Int = (0xFF shl 24) or (114 shl 16) or (114 shl 8) or 114,
    private val iouThreshold: Float = 0.45f
) : ModelStrategy {

    override fun preprocess(bitmap: Bitmap): PreprocessResult {
        val origWidth = bitmap.width
        val origHeight = bitmap.height

        val scale = min(inputSize.toFloat() / origWidth, inputSize.toFloat() / origHeight)
        val newUnpadWidth = (origWidth * scale).roundToInt()
        val newUnpadHeight = (origHeight * scale).roundToInt()

        val padX = (inputSize - newUnpadWidth) / 2f
        val padY = (inputSize - newUnpadHeight) / 2f

        val resized = bitmap.scale(newUnpadWidth, newUnpadHeight)

        val letterboxed = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        Canvas(letterboxed).apply {
            drawColor(letterboxColor)
            drawBitmap(resized, padX, padY, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val buffer = bitmapToNchwFloatBuffer(letterboxed)

        return PreprocessResult(
            inputBuffer = buffer,
            transform = LetterboxTransform(
                scale = scale,
                padX = padX,
                padY = padY,
                origWidth = origWidth,
                origHeight = origHeight
            )
        )
    }

    /**
     * Конвертация ARGB Bitmap в NCHW float32 ByteBuffer: сначала подряд все значения
     * канала R по всем пикселям, потом все G, потом все B — именно такого порядка
     * (не interleaved RGB) ждёт модель с input_shape=[1,3,H,W].
     *
     * Один проход по pixels вместо трёх — R/G/B каждого пикселя достаём сразу и
     * пишем по трём разным смещениям буфера (абсолютный putFloat(index, value),
     * без движения курсора), а не гоняем массив заново под каждый канал.
     */
    private fun bitmapToNchwFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val size = bitmap.width * bitmap.height
        val buffer = ByteBuffer.allocateDirect(4 * 3 * size).order(ByteOrder.nativeOrder())

        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val rOffsetBytes = 0
        val gOffsetBytes = 4 * size
        val bOffsetBytes = 8 * size

        for (i in 0 until size) {
            val pixel = pixels[i]
            val byteIndex = i * 4
            buffer.putFloat(rOffsetBytes + byteIndex, ((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(gOffsetBytes + byteIndex, ((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat(bOffsetBytes + byteIndex, (pixel and 0xFF) / 255f)
        }
        return buffer
    }

    override fun postprocess(
        rawOutput: Array<FloatArray>,
        transform: LetterboxTransform,
        confidenceThreshold: Float
    ): List<RawDetection> {
        val numAnchors = rawOutput[0].size

        // ВАЖНО (производительность): rawOutput — это Array<FloatArray>, то есть 159 отдельных
        // объектов в куче, а не один непрерывный блок памяти. Если идти по anchor во внешнем
        // цикле и classIdx во внутреннем — каждая итерация внутреннего цикла прыгает на другой
        // FloatArray-объект (155 разных мест в памяти на каждый anchor), что убивает кэш CPU.
        //
        // Вместо этого идём по classIdx во внешнем цикле — тогда внутренний цикл читает ОДИН
        // и тот же FloatArray последовательно (numAnchors идут подряд в памяти) — это ровно
        // тот порядок обхода, для которого аппаратный prefetcher процессора эффективен.
        val maxScore = FloatArray(numAnchors)
        val maxClassId = IntArray(numAnchors) { -1 }

        for (classIdx in 0 until numClasses) {
            val channelRow = rawOutput[4 + classIdx]
            for (anchor in 0 until numAnchors) {
                val score = channelRow[anchor]
                if (score > maxScore[anchor]) {
                    maxScore[anchor] = score
                    maxClassId[anchor] = classIdx
                }
            }
        }

        val candidates = ArrayList<Candidate>()

        for (anchor in 0 until numAnchors) {
            val score = maxScore[anchor]
            if (score <= confidenceThreshold) continue

            // Координаты нормализованы в [0,1] относительно inputSize — переводим в пиксели входа модели
            val cx = rawOutput[0][anchor] * inputSize
            val cy = rawOutput[1][anchor] * inputSize
            val w = rawOutput[2][anchor] * inputSize
            val h = rawOutput[3][anchor] * inputSize

            // Убираем letterbox-паддинг и масштаб — возвращаемся в пиксели ИСХОДНОГО кадра
            val x1 = ((cx - w / 2f - transform.padX) / transform.scale)
                .coerceIn(0f, transform.origWidth.toFloat())
            val y1 = ((cy - h / 2f - transform.padY) / transform.scale)
                .coerceIn(0f, transform.origHeight.toFloat())
            val x2 = ((cx + w / 2f - transform.padX) / transform.scale)
                .coerceIn(0f, transform.origWidth.toFloat())
            val y2 = ((cy + h / 2f - transform.padY) / transform.scale)
                .coerceIn(0f, transform.origHeight.toFloat())

            if (x2 <= x1 || y2 <= y1) continue // вырожденная рамка — пропускаем

            candidates += Candidate(classId = maxClassId[anchor], confidence = score, x1 = x1, y1 = y1, x2 = x2, y2 = y2)
        }

        val kept = nmsPerClass(candidates, iouThreshold)

        return kept.map { c ->
            RawDetection(
                classId = c.classId,
                confidence = c.confidence,
                // Нормализуем в [0,1] относительно исходного кадра — таков контракт RawDetection
                xMin = c.x1 / transform.origWidth,
                yMin = c.y1 / transform.origHeight,
                xMax = c.x2 / transform.origWidth,
                yMax = c.y2 / transform.origHeight
            )
        }
    }

    /**
     * Жадный NMS, раздельный по classId — знаки разных типов друг друга не гасят,
     * даже если их рамки сильно перекрываются (частый случай: несколько знаков на одном столбе).
     */
    private fun nmsPerClass(candidates: List<Candidate>, iouThreshold: Float): List<Candidate> {
        val result = ArrayList<Candidate>()

        for (classId in candidates.map { it.classId }.distinct()) {
            val sameClass = candidates.filter { it.classId == classId }.sortedByDescending { it.confidence }
            val suppressed = BooleanArray(sameClass.size)

            for (i in sameClass.indices) {
                if (suppressed[i]) continue
                result += sameClass[i]

                for (j in (i + 1) until sameClass.size) {
                    if (suppressed[j]) continue
                    if (calculateIou(sameClass[i], sameClass[j]) > iouThreshold) {
                        suppressed[j] = true
                    }
                }
            }
        }
        return result
    }

    private fun calculateIou(a: Candidate, b: Candidate): Float {
        val interX1 = max(a.x1, b.x1)
        val interY1 = max(a.y1, b.y1)
        val interX2 = min(a.x2, b.x2)
        val interY2 = min(a.y2, b.y2)

        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        val unionArea = areaA + areaB - interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    private data class Candidate(
        val classId: Int,
        val confidence: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    )
}