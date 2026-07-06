package com.example.feature_cv.tracker

import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.model.RawDetection
import kotlin.math.max
import kotlin.math.min

/**
 * Простой трекер на основе IoU-матчинга + TTL ("жизни").
 *
 * Алгоритм:
 * 1. Считаем IoU между каждой парой (существующий трек, новая детекция) ОДНОГО classId
 * 2. Жадно матчим пары с IoU >= iouThreshold, от большего к меньшему — раз трек/детекция
 *    уже кому-то сматчились, они больше не участвуют в дальнейшем матчинге этого кадра
 * 3. Совпавший трек -> обновляем lastDetection, livesLeft = maxLives
 * 4. Несовпавший трек -> livesLeft--, удаляем при livesLeft <= 0
 * 5. Несовпавшая детекция -> новый Track с новым id
 *
 * Не потокобезопасен — предполагается, что update() вызывается последовательно
 * с одного и того же диспетчера (как и InferenceEngine), что уже заложено в архитектуре.
 */
class IouSignTracker(
    private val iouThreshold: Float = 0.35f,
    private val maxLives: Int = 5
) : SignTracker {

    private val activeTracks = mutableListOf<Track>()
    private var nextId = 0

    override fun update(detections: List<RawDetection>): List<DetectedSign> {
        val possibleMatches = findPossibleMatches(detections)
        // Сначала самые уверенные совпадения — жадный матчинг
        possibleMatches.sortByDescending { it.iou }

        val matchedTrackIndices = mutableSetOf<Int>()
        val usedDetectionIndices = mutableSetOf<Int>()

        for (match in possibleMatches) {
            if (match.trackIndex in matchedTrackIndices) continue
            if (match.detectionIndex in usedDetectionIndices) continue

            matchedTrackIndices += match.trackIndex
            usedDetectionIndices += match.detectionIndex

            val track = activeTracks[match.trackIndex]
            track.lastDetection = detections[match.detectionIndex]
            track.livesLeft = maxLives
        }

        // Несматченные треки теряют жизнь (индексы ещё валидны — список пока не менялся структурно)
        for ((trackIndex, track) in activeTracks.withIndex()) {
            if (trackIndex !in matchedTrackIndices) {
                track.livesLeft -= 1
            }
        }
        activeTracks.removeAll { it.livesLeft <= 0 }

        // Несматченные детекции — новые физические знаки в кадре
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

    private data class MatchCandidate(val trackIndex: Int, val detectionIndex: Int, val iou: Float)

    private fun findPossibleMatches(detections: List<RawDetection>): MutableList<MatchCandidate> {
        val possibleMatches = mutableListOf<MatchCandidate>()
        for ((trackIndex, track) in activeTracks.withIndex()) {
            for ((detIndex, detection) in detections.withIndex()) {
                // Матчим только внутри одного класса — разные типы знаков друг с другом не путаем
                if (track.lastDetection.classId != detection.classId) continue

                val iou = calculateIou(track.lastDetection, detection)
                if (iou >= iouThreshold) {
                    possibleMatches += MatchCandidate(trackIndex, detIndex, iou)
                }
            }
        }
        return possibleMatches
    }

    /** Вычисляет Intersection over Union между двумя рамками. Чистая функция — легко тестировать. */
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