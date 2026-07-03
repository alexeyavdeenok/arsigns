package com.example.feature_cv.inference.strategies

import com.example.feature_cv.inference.ModelStrategy
import com.example.feature_cv.inference.model.RawDetection

/**
 * Реализация ModelStrategy под архитектуру YOLOv8 (выход формы [1, 4+num_classes, 8400]).
 *
 * TODO при реализации:
 * - preprocess: resize до inputSize x inputSize, нормализация в [0,1], нужный layout (NHWC/NCHW)
 * - postprocess: транспонирование выхода, фильтрация по confidenceThreshold, NMS (IoU ~0.45)
 */
class YoloV8Strategy(
    override val inputSize: Int,
    override val fileName: String
) : ModelStrategy {

    override fun preprocess(): Any {
        TODO("не реализовано")
    }

    override fun postprocess(confidenceThreshold: Float): List<RawDetection> {
        TODO("не реализовано")
    }
}
