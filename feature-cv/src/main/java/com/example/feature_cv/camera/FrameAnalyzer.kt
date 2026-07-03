package com.example.feature_cv.camera

/**
 * ImageAnalysis.Analyzer — получает "живые" кадры с камеры.
 *
 * Ответственность:
 * 1. Конвертировать ImageProxy (YUV_420_888) в формат, понятный InferenceEngine (Bitmap/TensorImage)
 * 2. Передать кадр в InferenceEngine на обработку
 * 3. Прогнать результат через SignTracker
 * 4. Обязательно закрыть imageProxy.close() сразу после копирования данных,
 *    иначе CameraX не пришлёт следующий кадр (backpressure застрянет)
 *
 * Пропуск кадров, пока YOLO занята, обеспечивается НЕ здесь вручную,
 * а настройкой ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
 * внутри CameraController — сюда просто не придёт лишний кадр.
 *
 * @param onResult callback в CvLayerApiImpl для обновления MutableStateFlow
 */
class FrameAnalyzer /* @Inject constructor(
    private val inferenceEngine: com.example.feature_cv.inference.InferenceEngine,
    private val tracker: com.example.feature_cv.tracker.SignTracker,
    private val confidenceThresholdProvider: () -> Float,
    private val onResult: (List<com.example.domain.model.DetectedSign>) -> Unit
) : ImageAnalysis.Analyzer */ {

    // override fun analyze(image: ImageProxy) {
    //     TODO: 1. конвертация image -> Bitmap/TensorImage
    //     TODO: 2. val raw = inferenceEngine.run(bitmap, confidenceThresholdProvider())
    //     TODO: 3. val tracked = tracker.update(raw)
    //     TODO: 4. onResult(tracked)
    //     TODO: 5. image.close() — ОБЯЗАТЕЛЬНО, иначе поток кадров остановится
    // }
}
