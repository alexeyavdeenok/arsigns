package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection
import kotlin.math.max
import kotlin.math.min

/**
 * Трекер с двухэтапным матчингом: IoU + "Спасение" по расстоянию.
 *
 * Решает проблему дубликатов при низком FPS (5-6 кадров/сек), когда из-за
 * большого смещения камеры IoU падает до 0, и трекер ошибочно создает новый ID.
 *
 * Производительность: O(N*M) простых арифметических операций.
 * Использование квадрата расстояния (без Math.sqrt) делает этот этап
 * быстрее, чем расчет IoU. Абсолютно безопасен для FPS.
 */
class RescueSignTracker(
    private val iouThreshold: Float = 0.35f,
    private val maxLives: Int = 4,
    private val velocitySmoothing: Float = 0.6f,
    /**
     * Множитель для порога расстояния "спасения".
     * 1.5f означает: если центр новой детекции находится ближе, чем 1.5 * max(ширина, высота)
     * предсказанной рамки, мы считаем это одним и тем же знаком.
     */
    private val rescueDistanceMultiplier: Float = 1.5f
) : SignTracker {

    private val activeTracks = mutableListOf<Track>()
    private var nextId = 0

    override fun update(detections: List<RawDetection>): List<DetectedSign> {
        // 1. Предсказываем позиции всех треков
        val predictedBoxes = activeTracks.map { predictBox(it) }

        // 2. ЭТАП 1: Стандартный строгий матчинг по IoU
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
            val newDetection = detections[match.detectionIndex]
            updateTrackState(track, newDetection)
        }

        // 3. ЭТАП 2: "Спасение" (Rescue) по расстоянию для несовпавших
        val unmatchedTracks = activeTracks.withIndex()
            .filter { it.index !in matchedTrackIndices }
            .map { it.value to it.index }

        val unmatchedDetections = detections.withIndex()
            .filter { it.index !in usedDetectionIndices }
            .map { it.value to it.index }

        for ((track, trackIndex) in unmatchedTracks) {
            var bestRescueDetection: Pair<RawDetection, Int>? = null
            var minSquaredDistance = Float.MAX_VALUE

            // Ищем ближайшую детекцию того же класса
            for ((detection, detIndex) in unmatchedDetections) {
                if (detection.classId != track.lastDetection.classId) continue

                val squaredDist = getSquaredCenterDistance(predictBox(track), detection)

                // Вычисляем динамический порог на основе размера предсказанной рамки
                val predictedWidth = predictBox(track).xMax - predictBox(track).xMin
                val predictedHeight = predictBox(track).yMax - predictBox(track).yMin
                val maxDimension = max(predictedWidth, predictedHeight)
                val thresholdSquared = (maxDimension * rescueDistanceMultiplier).let { it * it }

                if (squaredDist < thresholdSquared && squaredDist < minSquaredDistance) {
                    minSquaredDistance = squaredDist
                    bestRescueDetection = detection to detIndex
                }
            }

            // Если нашли подходящую детекцию для "спасения"
            if (bestRescueDetection != null) {
                val (rescueDetection, detIndex) = bestRescueDetection
                matchedTrackIndices += trackIndex
                usedDetectionIndices += detIndex
                updateTrackState(track, rescueDetection)
            }
        }

        // 4. Обновляем треки, которые так и не нашли пару (летят по инерции)
        for ((trackIndex, track) in activeTracks.withIndex()) {
            if (trackIndex !in matchedTrackIndices) {
                track.livesLeft -= 1
                track.lastDetection = shiftBox(track.lastDetection, track.velocityX, track.velocityY)
            }
        }

        // 5. Удаляем умершие треки
        activeTracks.removeAll { it.livesLeft <= 0 }

        // 6. Создаем новые треки для детекций, которые не подошли ни одному старому треку
        for ((detIndex, detection) in detections.withIndex()) {
            if (detIndex in usedDetectionIndices) continue
            activeTracks += Track(
                id = nextId++,
                lastDetection = detection,
                livesLeft = maxLives
            )
        }

        // 7. Формируем результат
        return activeTracks.map { track ->
            DetectedSign(
                id = track.id,
                confidence = track.lastDetection.confidence,
                xMin = track.lastDetection.xMin,
                yMin = track.lastDetection.yMin,
                xMax = track.lastDetection.xMax,
                yMax = track.lastDetection.yMax,
                classId = track.lastDetection.classId
            )
        }
    }

    /**
     * Обновляет состояние трека новыми данными: сбрасывает TTL, обновляет координаты
     * и пересчитывает скорость со сглаживанием.
     */
    private fun updateTrackState(track: Track, newDetection: RawDetection) {
        val measuredVx = centerX(newDetection) - centerX(track.lastDetection)
        val measuredVy = centerY(newDetection) - centerY(track.lastDetection)

        track.velocityX = track.velocityX * (1 - velocitySmoothing) + measuredVx * velocitySmoothing
        track.velocityY = track.velocityY * (1 - velocitySmoothing) + measuredVy * velocitySmoothing

        track.lastDetection = newDetection
        track.livesLeft = maxLives
    }

    /**
     * Вычисляет квадрат расстояния между центрами двух рамок.
     * Использование квадрата позволяет избежать дорогой операции Math.sqrt().
     */
    private fun getSquaredCenterDistance(a: RawDetection, b: RawDetection): Float {
        val dx = centerX(a) - centerX(b)
        val dy = centerY(a) - centerY(b)
        return dx * dx + dy * dy
    }

    private fun centerX(d: RawDetection) = (d.xMin + d.xMax) / 2f
    private fun centerY(d: RawDetection) = (d.yMin + d.yMax) / 2f

    private fun predictBox(track: Track): RawDetection =
        shiftBox(track.lastDetection, track.velocityX, track.velocityY)

    private fun shiftBox(d: RawDetection, dx: Float, dy: Float): RawDetection = d.copy(
        xMin = d.xMin + dx, xMax = d.xMax + dx,
        yMin = d.yMin + dy, yMax = d.yMax + dy
    )

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
}