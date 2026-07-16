package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection
import kotlin.math.max
import kotlin.math.min

/**
 * Трекер на основе фильтра Калмана — альтернатива IouSignTracker, отдельный класс,
 * чтобы можно было в любой момент переключиться обратно одной строкой в di/CvModule
 * (@Provides fun provideSignTracker(): SignTracker = IouSignTracker() / KalmanSignTracker()).
 *
 * УПРОЩЕНИЕ (важно понимать, что именно это даёт): здесь 4 НЕЗАВИСИМЫХ одномерных
 * фильтра Калмана на трек (по центру X, центру Y, ширине, высоте) — не единый
 * совместный 2D/4D фильтр с общей ковариационной матрицей, как в "настоящем" SORT.
 * Корреляция между осями (например "если знак смещается вправо, скорее всего он
 * одновременно немного приближается") не учитывается. Зато код простой и быстрый —
 * для дорожных знаков (плавное движение камеры, оси движутся более-менее независимо)
 * этого обычно достаточно; полная совместная модель — возможный следующий шаг.
 *
 * Kalman здесь даёт над constant-velocity-трекером с EMA: явную модель неопределённости
 * (P — ковариация), которая сама регулирует, насколько доверять новому измерению против
 * предсказания — в отличие от фиксированного коэффициента сглаживания EMA, это
 * адаптируется по ходу работы (после нескольких стабильных измерений фильтр "доверяет"
 * предсказанию больше, после промаха/шума — больше доверяет новому измерению).
 */
class KalmanSignTracker(
    private val iouThreshold: Float = 0.35f,
    private val maxLives: Int = 4,
    private val processNoisePosition: Float = 1e-4f,
    private val processNoiseVelocity: Float = 1e-4f,
    private val measurementNoise: Float = 0.01f
) : SignTracker {

    private val activeTracks = mutableListOf<KalmanTrack>()
    private var nextId = 0

    override fun update(detections: List<RawDetection>): List<DetectedSign> {
        // Шаг предсказания — двигаем состояние каждого трека на один шаг вперёд ДО матчинга
        activeTracks.forEach { it.predict(processNoisePosition, processNoiseVelocity) }

        val predictedBoxes = activeTracks.map { it.toRawDetection() }
        val possibleMatches = findPossibleMatches(predictedBoxes, detections)
        possibleMatches.sortByDescending { it.iou }

        val matchedTrackIndices = mutableSetOf<Int>()
        val usedDetectionIndices = mutableSetOf<Int>()

        for (match in possibleMatches) {
            if (match.trackIndex in matchedTrackIndices) continue
            if (match.detectionIndex in usedDetectionIndices) continue

            matchedTrackIndices += match.trackIndex
            usedDetectionIndices += match.detectionIndex

            val track = activeTracks[match.trackIndex]
            val detection = detections[match.detectionIndex]
            track.correct(detection, measurementNoise)
            track.livesLeft = maxLives
        }

        for ((trackIndex, track) in activeTracks.withIndex()) {
            if (trackIndex !in matchedTrackIndices) {
                track.livesLeft -= 1
                // Позиция уже продвинута шагом predict() выше — трек "по инерции"
                // продолжает двигаться по своей модели, пока жив по TTL.
            }
        }
        activeTracks.removeAll { it.livesLeft <= 0 }

        for ((detIndex, detection) in detections.withIndex()) {
            if (detIndex in usedDetectionIndices) continue
            activeTracks += KalmanTrack.initial(id = nextId++, detection = detection, livesLeft = maxLives)
        }

        return activeTracks.map { it.toDetectedSign() }
    }

    private data class MatchCandidate(val trackIndex: Int, val detectionIndex: Int, val iou: Float)

    private fun findPossibleMatches(
        predictedBoxes: List<RawDetection>,
        detections: List<RawDetection>
    ): MutableList<MatchCandidate> {
        val possibleMatches = mutableListOf<MatchCandidate>()
        for ((trackIndex, predicted) in predictedBoxes.withIndex()) {
            for ((detIndex, detection) in detections.withIndex()) {
                if (predicted.classId != detection.classId) continue
                val iou = calculateIou(predicted, detection)
                if (iou >= iouThreshold) {
                    possibleMatches += MatchCandidate(trackIndex, detIndex, iou)
                }
            }
        }
        return possibleMatches
    }

    private fun calculateIou(a: RawDetection, b: RawDetection): Float {
        val interX1 = max(a.xMin, b.xMin)
        val interY1 = max(a.yMin, b.yMin)
        val interX2 = min(a.xMax, b.xMax)
        val interY2 = min(a.yMax, b.yMax)

        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        val areaA = (a.xMax - a.xMin) * (a.yMax - a.yMin)
        val areaB = (b.xMax - b.xMin) * (b.yMax - b.yMin)
        val unionArea = areaA + areaB - interArea

        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    /** Внутреннее состояние трека — 4 независимых 1D-фильтра Калмана + метаданные. */
    private class KalmanTrack(
        val id: Int,
        var classId: Int,
        var confidence: Float,
        var livesLeft: Int,
        private val cx: Kalman1D,
        private val cy: Kalman1D,
        private val w: Kalman1D,
        private val h: Kalman1D
    ) {
        fun predict(processNoisePosition: Float, processNoiseVelocity: Float) {
            cx.predict(processNoisePosition, processNoiseVelocity)
            cy.predict(processNoisePosition, processNoiseVelocity)
            w.predict(processNoisePosition, processNoiseVelocity)
            h.predict(processNoisePosition, processNoiseVelocity)
        }

        fun correct(detection: RawDetection, measurementNoise: Float) {
            cx.correct(centerX(detection), measurementNoise)
            cy.correct(centerY(detection), measurementNoise)
            w.correct(detection.xMax - detection.xMin, measurementNoise)
            h.correct(detection.yMax - detection.yMin, measurementNoise)
            classId = detection.classId
            confidence = detection.confidence
        }

        fun toRawDetection(): RawDetection {
            val halfW = w.position() / 2f
            val halfH = h.position() / 2f
            return RawDetection(
                classId = classId,
                confidence = confidence,
                xMin = cx.position() - halfW,
                yMin = cy.position() - halfH,
                xMax = cx.position() + halfW,
                yMax = cy.position() + halfH
            )
        }

        fun toDetectedSign(): DetectedSign {
            val raw = toRawDetection()
            return DetectedSign(
                id = id,
                confidence = raw.confidence,
                xMin = raw.xMin,
                yMin = raw.yMin,
                xMax = raw.xMax,
                yMax = raw.yMax,
                classId = raw.classId
            )
        }

        companion object {
            private fun centerX(d: RawDetection) = (d.xMin + d.xMax) / 2f
            private fun centerY(d: RawDetection) = (d.yMin + d.yMax) / 2f

            fun initial(id: Int, detection: RawDetection, livesLeft: Int): KalmanTrack = KalmanTrack(
                id = id,
                classId = detection.classId,
                confidence = detection.confidence,
                livesLeft = livesLeft,
                cx = Kalman1D(centerX(detection)),
                cy = Kalman1D(centerY(detection)),
                w = Kalman1D(detection.xMax - detection.xMin),
                h = Kalman1D(detection.yMax - detection.yMin)
            )
        }
    }

    /**
     * Одномерный фильтр Калмана с состоянием [позиция, скорость] и моделью постоянной
     * скорости (F = [[1,1],[0,1]]). Измеряется только позиция (H = [1,0]) — скорость
     * фильтр оценивает сам по динамике изменения позиции между измерениями.
     */
    private class Kalman1D(initialPosition: Float) {
        private var pos = initialPosition
        private var vel = 0f

        // Ковариационная матрица P = [[p00,p01],[p01,p11]] (симметричная)
        private var p00 = 1f
        private var p01 = 0f
        private var p11 = 1f

        fun predict(processNoisePos: Float, processNoiseVel: Float) {
            val newPos = pos + vel
            val newVel = vel

            val newP00 = p00 + 2 * p01 + p11 + processNoisePos
            val newP01 = p01 + p11
            val newP11 = p11 + processNoiseVel

            pos = newPos
            vel = newVel
            p00 = newP00
            p01 = newP01
            p11 = newP11
        }

        fun correct(measurement: Float, measurementNoise: Float) {
            val innovation = measurement - pos
            val s = p00 + measurementNoise
            val k0 = p00 / s
            val k1 = p01 / s

            pos += k0 * innovation
            vel += k1 * innovation

            val newP00 = (1 - k0) * p00
            val newP01 = (1 - k0) * p01
            val newP11 = p11 - k1 * p01

            p00 = newP00
            p01 = newP01
            p11 = newP11
        }

        fun position(): Float = pos
    }
}