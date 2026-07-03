package com.example.feature_cv.inference.model

/**
 * "Сырая" детекция сразу после постпроцессинга модели (NMS уже применена),
 * ДО трекера. У неё ещё нет стабильного id и TTL — это забота SignTracker.
 *
 * Отличие от com.example.domain.model.DetectedSign:
 * DetectedSign — то, что видит :app (уже с id трекера).
 * RawDetection — внутренний формат :feature-cv, между InferenceEngine и SignTracker.
 */
data class RawDetection(
    val yoloClassIndex: String,
    val confidence: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)
