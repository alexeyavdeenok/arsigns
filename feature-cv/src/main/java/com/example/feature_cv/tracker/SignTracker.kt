package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection

/**
 * Контракт трекера. Позволяет заменить реализацию (например на ByteTrack-подобную)
 * одной строкой в DI-модуле (di/CvModule), не трогая остальной пайплайн.
 *
 * Задачи реализации:
 * - сматчить новые детекции с треками из прошлого кадра (обычно по IoU)
 * - совпавшим трекам сбросить livesLeft, обновить координаты
 * - несовпавшим трекам уменьшить livesLeft, при 0 — удалить
 * - несовпавшим детекциям создать новый трек с новым id
 *
 * @see IouSignTracker дефолтная реализация для v1.0
 */
interface SignTracker {
    /**
     * @param detections сырые детекции текущего кадра от InferenceEngine
     * @return стабильный список знаков с id трекера — то, что уходит в CvLayerApi.liveDetectedSigns
     */
    fun update(detections: List<RawDetection>): List<DetectedSign>
}
