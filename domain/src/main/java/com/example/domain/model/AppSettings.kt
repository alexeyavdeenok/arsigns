package com.example.domain.model

enum class YoloModelType(
    val fileName: String,
    val uiName: String,
    val inputSize: Int
) {
    YOLO_V8_640("yolov8n_640.tflite", "YOLOv8 640", 640),
    YOLO_V8_416("yolov8n_416.tflite", "YOLOv8 416", 416),
    YOLO_V8_224("yolov8n_224.tflite", "YOLOv8 224", 224);


    companion object {
        fun fromStoredValue(value: String?): YoloModelType {
            if (value.isNullOrBlank()) return YOLO_V8_640

            return entries.firstOrNull { it.name == value }
                ?: entries.firstOrNull { it.fileName == value }
                ?: YOLO_V8_640
        }
    }
}

data class AppSettings(
    val isVoiceAlertsEnabled: Boolean = true,
    val yoloConfidenceThreshold: Float = 0.5f,
    val selectedModel: YoloModelType = YoloModelType.YOLO_V8_640
)
