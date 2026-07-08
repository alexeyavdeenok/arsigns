package com.example.feature_cv.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.DetectedSign
import com.example.feature_cv.inference.InferenceEngine
import com.example.feature_cv.tracker.SignTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import androidx.core.graphics.createBitmap

class FrameAnalyzer(
    private val inferenceEngine: InferenceEngine,
    private val tracker: SignTracker,
    private val inferenceDispatcher: CoroutineDispatcher,
    private val confidenceThresholdProvider: () -> Float,
    private val onResult: (signs: List<DetectedSign>, frameWidth: Int, frameHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = imageProxyToUprightBitmap(image)

            val detectedSigns = runBlocking(inferenceDispatcher) {
                val rawDetections = inferenceEngine.run(bitmap, confidenceThresholdProvider())
                tracker.update(rawDetections)
            }

            onResult(detectedSigns, bitmap.width, bitmap.height)
        } finally {
            image.close()
        }
    }

    private fun imageProxyToUprightBitmap(image: ImageProxy): Bitmap {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPaddingPixels = (rowStride - pixelStride * image.width) / pixelStride

        val rawBitmap = createBitmap(image.width + rowPaddingPixels, image.height)
        rawBitmap.copyPixelsFromBuffer(plane.buffer)

        val bitmap = if (rowPaddingPixels == 0) {
            rawBitmap
        } else {
            Bitmap.createBitmap(rawBitmap, 0, 0, image.width, image.height)
        }

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}