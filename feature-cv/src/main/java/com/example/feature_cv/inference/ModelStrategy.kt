package com.example.feature_cv.inference

import com.example.feature_cv.inference.model.RawDetection

/**
 * Контракт "одна модель = одна стратегия препроцессинга/постпроцессинга".
 * Позволяет добавлять новые архитектуры моделей, не трогая InferenceEngine.
 *
 * Каждая реализация знает:
 * - под какой inputSize (640/416/224) заточена модель
 * - как привести Bitmap к тензору нужного формата
 * - как декодировать сырой выход модели в список детекций (включая NMS)
 *
 * @see ModelStrategyFactory создаёт нужную реализацию по YoloModelType из настроек
 */
interface ModelStrategy {

    /** Размер стороны входного квадратного изображения (например 640) */
    val inputSize: Int

    /** Имя файла модели в assets (например "yolov8n_640_fp16.tflite") */
    val fileName: String

    /**
     * Привести кадр к формату входного тензора модели (resize, normalize, layout).
     */
    fun preprocess(/* bitmap: Bitmap */): Any /* ByteBuffer */

    /**
     * Декодировать сырой выход модели в список детекций.
     * Обязана включать Non-Max Suppression.
     *
     * @param confidenceThreshold берётся из AppSettings.yoloConfidenceThreshold
     */
    fun postprocess(
        /* rawOutput: Array<FloatArray> */
        confidenceThreshold: Float
    ): List<RawDetection>
}
