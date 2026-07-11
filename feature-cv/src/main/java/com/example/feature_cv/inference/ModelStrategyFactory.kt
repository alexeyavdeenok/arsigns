package com.example.feature_cv.inference

import com.example.domain.model.YoloModelType
import com.example.feature_cv.inference.strategies.YoloV26Strategy
import com.example.feature_cv.inference.strategies.YoloV8Strategy
import javax.inject.Inject

/**
 * Фабрика ModelStrategy по выбранной в настройках модели.
 * Единственное место, которое нужно поправить при добавлении новой модели/архитектуры.
 *
 * @see com.example.domain.model.YoloModelType enum с вариантами из настроек
 */
class ModelStrategyFactory @Inject constructor()  {

    /**
     * @return готовую стратегию под конкретный YoloModelType.
     * Пока все варианты — YOLOv8 с разным inputSize, поэтому одна реализация на всех.
     * При появлении другой архитектуры — добавить ветку с другим ModelStrategy.
     */
    fun create(type: YoloModelType): ModelStrategy = when (type) {
        YoloModelType.YOLO_V8_640,
        YoloModelType.YOLO_V8_416,
        YoloModelType.YOLO_V8_224 ->
            YoloV8Strategy(inputSize = type.inputSize, fileName = type.fileName)

        YoloModelType.YOLO26_640,
        YoloModelType.YOLO26_416,
        YoloModelType.YOLO26_224 ->
            YoloV26Strategy(inputSize = type.inputSize, fileName = type.fileName)
    }
}
