package com.example.feature_cv.inference

import com.example.domain.model.YoloModelType
import com.example.feature_cv.inference.strategies.YoloV8Strategy

/**
 * Фабрика ModelStrategy по выбранной в настройках модели.
 * Единственное место, которое нужно поправить при добавлении новой модели/архитектуры.
 *
 * @see com.example.domain.model.YoloModelType enum с вариантами из настроек
 */
class ModelStrategyFactory /* @Inject constructor() */ {

    /**
     * @return готовую стратегию под конкретный YoloModelType.
     * Пока все варианты — YOLOv8 с разным inputSize, поэтому одна реализация на всех.
     * При появлении другой архитектуры — добавить ветку с другим ModelStrategy.
     */
    fun create(type: YoloModelType): ModelStrategy {
        return YoloV8Strategy(inputSize = type.inputSize, fileName = type.fileName)
    }
}
