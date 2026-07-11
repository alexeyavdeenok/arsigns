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
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Реализация ModelStrategy под YOLO26, экспортированную в TFLite через LiteRT
 * (litert_torch) напрямую из PyTorch.
 *
 * Проверенная конвенция (см. Python-диагностику перед переносом в Kotlin):
 * - Layout входа: NCHW [1, 3, inputSize, inputSize] — совпадает с YOLOv8,
 *   preprocess() ниже — копия соответствующего метода YoloV8Strategy
 *   (сознательно не вынесено в общий helper — см. пояснение в чате).
 * - Выход: [1, 300, 6] — на каждую из 300 строк [x1, y1, x2, y2, confidence, classId].
 *   Модель one-to-one head — end-to-end, NMS-free: раздельный NMS по классам
 *   (как в YoloV8Strategy) здесь НЕ нужен, дубликаты уже отфильтрованы моделью.
 * - !!! ПРОВЕРЬ НА СВОЁМ ЭКСПОРТЕ !!!: координаты x1..y2 могут быть либо
 *   нормализованы в [0,1] относительно inputSize, либо уже в пикселях inputSize.
 *   Ниже — автоопределение по максимальному значению координаты (тот же приём,
 *   что в Python-скрипте). Если на твоём экспорте всегда один и тот же вариант —
 *   надёжнее убрать автоопределение и зашить константой, см. TODO ниже.
 */
class YoloV26Strategy(
    override val inputSize: Int,
    override val fileName: String,
    private val letterboxColor: Int = (0xFF shl 24) or (114 shl 16) or (114 shl 8) or 114
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

        return PreprocessResult(
            inputBuffer = bitmapToNchwFloatBuffer(letterboxed),
            transform = LetterboxTransform(
                scale = scale, padX = padX, padY = padY,
                origWidth = origWidth, origHeight = origHeight
            )
        )
    }

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
        // rawOutput: [300][6] -> x1, y1, x2, y2, confidence, classId
        val result = ArrayList<RawDetection>()

        for (row in rawOutput) {
            val confidence = row[4]
            if (confidence < confidenceThreshold) continue // "пустые" слоты из 300 отсекаются здесь

            var x1 = row[0]
            var y1 = row[1]
            var x2 = row[2]
            var y2 = row[3]
            val classId = row[5].toInt()

            // TODO: если знаешь точно, нормализован выход или нет — замени эту проверку
            // на константу (isNormalizedOutput = true/false) вместо автоопределения.
            val looksNormalized = x2 <= 1.5f && y2 <= 1.5f
            if (looksNormalized) {
                x1 *= inputSize; y1 *= inputSize
                x2 *= inputSize; y2 *= inputSize
            }

            val origX1 = ((x1 - transform.padX) / transform.scale)
                .coerceIn(0f, transform.origWidth.toFloat())
            val origY1 = ((y1 - transform.padY) / transform.scale)
                .coerceIn(0f, transform.origHeight.toFloat())
            val origX2 = ((x2 - transform.padX) / transform.scale)
                .coerceIn(0f, transform.origWidth.toFloat())
            val origY2 = ((y2 - transform.padY) / transform.scale)
                .coerceIn(0f, transform.origHeight.toFloat())

            if (origX2 <= origX1 || origY2 <= origY1) continue

            result += RawDetection(
                classId = classId,
                confidence = confidence,
                xMin = origX1 / transform.origWidth,
                yMin = origY1 / transform.origHeight,
                xMax = origX2 / transform.origWidth,
                yMax = origY2 / transform.origHeight
            )
        }

        return result
    }
}