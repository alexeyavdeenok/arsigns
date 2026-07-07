package com.example.feature_cv.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.InferenceEngine
import com.example.feature_cv.tracker.SignTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import androidx.core.graphics.createBitmap

/**
 * ImageAnalysis.Analyzer — получает "живые" кадры с камеры.
 *
 * Пропуск кадров, пока YOLO занята, обеспечивает CameraX сам (STRATEGY_KEEP_ONLY_LATEST
 * в CameraController) — сюда просто не придёт лишний кадр, пока не вернётся analyze().
 *
 * ВАЖНО про потоки: analyze() вызывается на CameraDispatcher (см. DispatcherModule).
 * runBlocking(inferenceDispatcher) внутри намеренно блокирует ЭТОТ поток до завершения
 * инференса — у CameraDispatcher нет другой работы, кроме прогона кадров через этот
 * analyzer, так что блокировка безвредна, а сам interpreter.run() физически исполнится
 * на InferenceThread (единственном потоке, где вообще можно трогать Interpreter).
 */
class FrameAnalyzer(
    private val inferenceEngine: InferenceEngine,
    private val tracker: SignTracker,
    private val inferenceDispatcher: CoroutineDispatcher,
    private val confidenceThresholdProvider: () -> Float,
    private val onResult: (List<DetectedSign>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = imageProxyToUprightBitmap(image)

            val detectedSigns = runBlocking(inferenceDispatcher) {
                val rawDetections = inferenceEngine.run(bitmap, confidenceThresholdProvider())
                tracker.update(rawDetections)
            }

            onResult(detectedSigns)
        } finally {
            image.close() // ОБЯЗАТЕЛЬНО — иначе CameraX не пришлёт следующий кадр
        }
    }

    /**
     * ImageProxy в формате RGBA_8888 (настроено в CameraController через
     * setOutputImageFormat) -> Bitmap, с поворотом под реальную ориентацию кадра.
     *
     * RGBA_8888 output format у CameraX специально совпадает по раскладке байт в памяти
     * с Bitmap.Config.ARGB_8888 — поэтому можно скопировать буфер напрямую через
     * copyPixelsFromBuffer(), без ручной YUV->RGB конвертации. rowStride может быть больше
     * width*pixelStride из-за паддинга буфера — учитываем и обрезаем при необходимости.
     */
    private fun imageProxyToUprightBitmap(image: ImageProxy): Bitmap {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride // 4 байта на пиксель для RGBA_8888
        val rowStride = plane.rowStride
        val rowPaddingPixels = (rowStride - pixelStride * image.width) / pixelStride

        val rawBitmap = createBitmap(image.width + rowPaddingPixels, image.height)
        rawBitmap.copyPixelsFromBuffer(plane.buffer)

        val bitmap = if (rowPaddingPixels == 0) {
            rawBitmap
        } else {
            // Обрезаем паддинг по краю строки, если буфер шире, чем реальная картинка
            Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
        }

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}