package com.example.domain.model

data class DetectedSign(
    val id: Int,
    val confidence: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val yoloClassIndex: String
)
