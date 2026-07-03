package com.example.domain.api

import com.example.domain.model.DetectedSign
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс взаимодействия с модулем компьютерного зрения (:feature-cv).
 * Этот «трафарет» слушает основной модуль (:app), чтобы рисовать рамки на экране.
 */
interface CvLayerApi {

    /**
     * Поток «живых» распознанных знаков в реальном времени.
     * Твоя YOLO + трекер непрерывно (30 FPS) обновляют этот список,
     * а :app модуль подписывается на него и рисует Bounding Box'ы поверх камеры.
     */
    val liveDetectedSigns: StateFlow<List<DetectedSign>>

    /**
     * Включить обработку кадров с камеры (запуск YOLO)
     */
    fun startDetection()

    /**
     * Поставить детекцию на паузу (например, когда свернули приложение или открыли меню)
     */
    fun stopDetection()
}
