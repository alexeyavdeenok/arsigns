package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection

/**
 * Простой трекер на основе IoU-матчинга + TTL ("жизни").
 *
 * Алгоритм (см. обсуждение архитектуры):
 * 1. Посчитать IoU между всеми парами (текущая детекция, трек из прошлого кадра)
 * 2. Жадно сматчить пары с IoU выше iouThreshold, от большего к меньшему
 * 3. Совпавший трек -> обновить координаты, livesLeft = maxLives
 * 4. Несовпавший трек -> livesLeft--, удалить если livesLeft <= 0
 * 5. Несовпавшая детекция -> новый Track с новым id (nextId++)
 *
 * Чистая логика, без Android-зависимостей — покрывается обычными JUnit-тестами
 * (см. src/test/.../tracker/IouSignTrackerTest.kt).
 *
 * @param iouThreshold порог совпадения рамок (например 0.35)
 * @param maxLives сколько кадров трек может прожить без подтверждения (например 5)
 */
class IouSignTracker(
    private val iouThreshold: Float = 0.35f,
    private val maxLives: Int = 5
) : SignTracker {

    // TODO: private val activeTracks = mutableListOf<Track>()
    // TODO: private var nextId = 0

    override fun update(detections: List<RawDetection>): List<DetectedSign> {
        // TODO: 1. посчитать IoU-матрицу между detections и activeTracks
        // TODO: 2. жадный матчинг по iouThreshold
        // TODO: 3. обновить/удалить/создать треки (см. KDoc выше)
        // TODO: 4. смапить activeTracks -> List<DetectedSign>
        TODO("не реализовано")
    }

    /** Вычисляет Intersection over Union между двумя рамками. Чистая функция — легко тестировать. */
    private fun calculateIou(a: RawDetection, b: RawDetection): Float {
        TODO("не реализовано")
    }
}
