package com.example.feature_cv.inference

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.feature_cv.inference.delegate.DelegateProvider
import com.example.feature_cv.inference.model.RawDetection
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InferenceEngine @Inject constructor(
    private val delegateProvider: DelegateProvider,
    @ApplicationContext private val context: Context
) {

    private var interpreter: Interpreter? = null
    private var currentStrategy: ModelStrategy? = null
    private var currentDelegateSetup: com.example.feature_cv.inference.delegate.DelegateSetup? = null

    private var outputShape: IntArray? = null

    fun load(strategy: ModelStrategy) {
        releaseCurrent()

        val modelBuffer = loadModelFile(strategy.fileName)
        val delegateSetup = delegateProvider.createDelegateSetup()

        val newInterpreter = Interpreter(modelBuffer, delegateSetup.options)
        newInterpreter.allocateTensors()

        interpreter = newInterpreter
        currentStrategy = strategy
        currentDelegateSetup = delegateSetup
        outputShape = newInterpreter.getOutputTensor(0).shape()

        android.util.Log.d(
            TAG,
            "load(${strategy.fileName}): inputShape=${newInterpreter.getInputTensor(0).shape().toList()}, " +
                    "outputShape=${outputShape?.toList()}"
        )
    }

    companion object {
        private const val TAG = "InferenceEngine"
    }

    fun run(bitmap: Bitmap, confidenceThreshold: Float): List<RawDetection> {
        val strategy = currentStrategy
            ?: error("InferenceEngine.run() вызван до load() — модель не загружена")
        val interp = interpreter
            ?: error("InferenceEngine: interpreter отсутствует, хотя strategy установлена — неконсистентное состояние")
        val shape = outputShape
            ?: error("InferenceEngine: output shape неизвестен")

        val preprocessResult = strategy.preprocess(bitmap)

        val channels = shape[1]
        val numAnchors = shape[2]
        val outputWithBatch = Array(1) { Array(channels) { FloatArray(numAnchors) } }

        interp.run(preprocessResult.inputBuffer, outputWithBatch)

        val rawOutput = outputWithBatch[0]

        return strategy.postprocess(rawOutput, preprocessResult.transform, confidenceThreshold)
    }

    fun close() {
        releaseCurrent()
    }

    private fun releaseCurrent() {
        interpreter?.close()
        interpreter = null
        currentStrategy = null
        outputShape = null

        currentDelegateSetup?.closeableDelegate?.close()
        currentDelegateSetup = null
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        return assetFileDescriptor.use { afd ->
            FileInputStream(afd.fileDescriptor).use { inputStream ->
                inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
            }
        }
    }
}