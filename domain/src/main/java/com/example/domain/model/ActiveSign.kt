package com.example.domain.model

data class ActiveSign(
    val id: Int,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val confidence: Float
)
