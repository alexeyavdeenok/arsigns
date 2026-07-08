package com.example.domain.api

import com.example.domain.model.DetectedSign
import com.example.domain.model.FrameSize
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс взаимодействия с модулем компьютерного зрения (:feature-cv).
 */
interface CvLayerApi {

    /**
     * Поток «живых» распознанных знаков в реальном времени.
     * Координаты нормализованы [0,1] относительно frameSize (см. ниже) — НЕ относительно
     * экрана и не относительно входа модели. :app обязан пересчитать их через frameSize,
     * а не умножать напрямую на размер Canvas/экрана — иначе рамки исказятся при
     * несовпадении aspect ratio кадра и экрана (см. обсуждение растянутых рамок).
     */
    val liveDetectedSigns: StateFlow<List<DetectedSign>>

    /**
     * Размер кадра (после поворота в правильную ориентацию), к которому относятся
     * нормализованные координаты в liveDetectedSigns. Null, пока не обработан ни один кадр.
     */
    val frameSize: StateFlow<FrameSize?>

    fun startDetection()
    fun stopDetection()
}