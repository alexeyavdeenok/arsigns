package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection
import kotlin.math.max
import kotlin.math.min

/**
 * IoU-трекер с предсказанием движения (constant-velocity model) + TTL.
 *
 * Отличие от "наивного" IoU-матчинга: вместо сравнения новой детекции с ПОСЛЕДНЕЙ
 * известной позицией трека, сравниваем с ПРЕДСКАЗАННОЙ позицией (последняя позиция +
 * скорость). При быстром движении камеры реальное смещение знака между двумя
 * обработанными кадрами может быть большим — без предсказания IoU падает ниже порога
 * и трек рвётся (создаётся новый id вместо продолжения старого). Предсказание "подгоняет"
 * ожидаемую позицию под направление движения, и IoU остаётся высоким даже при смещении.
 *
 * Это упрощённая версия того же принципа, на котором построены SORT/DeepSORT —
 * там используется полноценный фильтр Калмана (учитывает шум измерений, ускорение),
 * здесь — линейная экстраполяция по скорости последнего измеренного смещения со
 * сглаживанием (EMA). Дешевле в вычислениях, менее точно на резких разворотах/ускорениях,
 * но для дорожных знаков (плавное движение камеры) обычно достаточно.
 *
 * Производительность: вся математика — O(activeTracks x detections) сравнений IoU плюс
 * O(activeTracks) на предсказание, то есть микросекунды при типичных 5-15 активных
 * треках. Это НЕ влияет на FPS всего пайплайна — узкое место остаётся в инференсе модели
 * (сотни миллисекунд), трекер на его фоне бесплатен что с предсказанием, что без.
 */
class IouSignTracker(
    private val iouThreshold: Float = 0.35f,
    private val maxLives: Int = 4,
    /** 0 = полностью игнорировать новое измерение скорости, 1 = не сглаживать вообще */
    private val velocitySmoothing: Float = 0.6f
) : SignTracker {

    private val activeTracks = mutableListOf<Track>()
    private var nextId = 0

    override fun update(detections: List<RawDetection>): List<DetectedSign> {
        val predictedBoxes = activeTracks.map { predictBox(it) }

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

            val measuredVx = centerX(newDetection) - centerX(track.lastDetection)
            val measuredVy = centerY(newDetection) - centerY(track.lastDetection)
            track.velocityX = track.velocityX * (1 - velocitySmoothing) + measuredVx * velocitySmoothing
            track.velocityY = track.velocityY * (1 - velocitySmoothing) + measuredVy * velocitySmoothing

            track.lastDetection = newDetection
            track.livesLeft = maxLives
        }

        for ((trackIndex, track) in activeTracks.withIndex()) {
            if (trackIndex !in matchedTrackIndices) {
                track.livesLeft -= 1
                track.lastDetection = shiftBox(track.lastDetection, track.velocityX, track.velocityY)
            }
        }
        activeTracks.removeAll { it.livesLeft <= 0 }

        for ((detIndex, detection) in detections.withIndex()) {
            if (detIndex in usedDetectionIndices) continue
            activeTracks += Track(id = nextId++, lastDetection = detection, livesLeft = maxLives)
        }

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