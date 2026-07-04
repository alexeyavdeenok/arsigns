package com.example.domain.model

data class ActiveSign(
    val trackerId: Int,        // id из DetectedSign — идентифицирует ЭКЗЕМПЛЯР знака в кадре
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val confidence: Float,
    val classId: Int, // оставляем на всякий случай, как ты и предлагал — дёшево, вдруг понадобится для дебага/аналитики
    val sign: SignEntity        // вся инфа о типе знака из каталога
)