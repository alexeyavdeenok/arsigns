package com.example.feature_cv.tracker

import com.example.feature_cv.inference.model.RawDetection

/**
 * Внутреннее состояние одного отслеживаемого трека между кадрами.
 * Не путать с DetectedSign (публичная модель для :app) — Track хранится
 * только внутри SignTracker и наружу не выходит.
 */
data class Track(
    val id: Int,
    var lastDetection: RawDetection,
    /** Сколько кадров подряд трек может прожить без совпадения, прежде чем удалится */
    var livesLeft: Int
)
