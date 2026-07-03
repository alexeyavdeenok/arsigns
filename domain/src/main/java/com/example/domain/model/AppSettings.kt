package com.example.domain.model

data class AppSettings(
    val confidenceThreshold: Float = 0.5f,
    val activeModelPath: String = "yolov8n.tflite",
    val isTtsEnabled: Boolean = true
)
