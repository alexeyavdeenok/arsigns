package com.example.domain.model

data class ActiveSign(
    val signId: Int,
    val confidence: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)
