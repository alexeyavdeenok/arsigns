package com.example.feature_cv.inference

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.feature_cv.inference.delegate.DelegateProvider
import com.example.feature_cv.inference.strategies.YoloV26Strategy
import com.example.feature_cv.inference.strategies.YoloV8Strategy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальный тест — нужен реальный девайс/эмулятор (androidTest, не test),
 * потому что тут участвует настоящий Interpreter + настоящая GPU/NNAPI/CPU выдача.
 */
@RunWith(AndroidJUnit4::class)
class InferenceEngineBenchmarkTest {

    companion object {
        private const val TEST_IMAGE_FILE_NAME = "images-7.jpeg"
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val N_WARMUP = 5
        private const val N_TIMED_RUNS = 30
    }

    private lateinit var engine: InferenceEngine
    private lateinit var testBitmap: Bitmap

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val delegateProvider = DelegateProvider(context)
        engine = InferenceEngine(delegateProvider, context)

        val testContext = InstrumentationRegistry.getInstrumentation().context
        testContext.assets.open(TEST_IMAGE_FILE_NAME).use { inputStream ->
            testBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    @Test
    fun engineProducesValidDetectionsOnRealImage() {
        // Для этого базового теста по умолчанию загрузим модель 640
        val strategy = YoloV8Strategy(inputSize = 640, fileName = "temp_model_640.tflite")
        engine.load(strategy)

        val detections = engine.run(testBitmap, CONFIDENCE_THRESHOLD)

        detections.forEach { d ->
            assertTrue("confidence должен быть в (threshold, 1]: ${d.confidence}", d.confidence > CONFIDENCE_THRESHOLD && d.confidence <= 1f)
            assertTrue("xMin < xMax", d.xMin < d.xMax)
            assertTrue("yMin < yMax", d.yMin < d.yMax)
            assertTrue("координаты нормализованы в [0,1]", d.xMin >= 0f && d.xMax <= 1f && d.yMin >= 0f && d.yMax <= 1f)
        }
    }

    @Test
    fun measureAverageInferenceTime() {
        // Список конфигураций для последовательного тестирования производительности
        val modelsToBenchmark = listOf(
            YoloV8Strategy(inputSize = 640, fileName = "temp_model_640.tflite"),
            YoloV8Strategy(inputSize = 416, fileName = "temp_model_416.tflite"),
            YoloV8Strategy(inputSize = 224, fileName = "temp_model_224.tflite")
        )

        println("\n=== НАЧАЛО СЕРИИ БЕНЧМАРКОВ ===")

        for (strategy in modelsToBenchmark) {
            // Переключаем движок на нужную модель перед прогревом и замером
            engine.load(strategy)
            runBenchmarkForStrategy(strategy)
        }

        println("=== КОНЕЦ СЕРИИ БЕНЧМАРКОВ ===\n")
    }

    private fun runBenchmarkForStrategy(strategy: ModelStrategy) {
        // Прогрев — первые вызовы обычно медленнее (JIT/делегат "прогревается")
        repeat(N_WARMUP) { engine.run(testBitmap, CONFIDENCE_THRESHOLD) }

        val timingsMs = mutableListOf<Long>()
        repeat(N_TIMED_RUNS) {
            val start = System.nanoTime()
            engine.run(testBitmap, CONFIDENCE_THRESHOLD)
            timingsMs += (System.nanoTime() - start) / 1_000_000
        }

        val avgMs = timingsMs.average()
        val sorted = timingsMs.sorted()
        val p95Ms = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]
        val estimatedFps = 1000.0 / avgMs

        println("--------------------------------------------------")
        println("=== Результаты для: ${strategy.fileName} (${strategy.inputSize}x${strategy.inputSize}) ===")
        println("Среднее время инференса: ${"%.1f".format(avgMs)} мс")
        println("p95 время инференса     : $p95Ms мс")
        println("Оценочный FPS           : ${"%.1f".format(estimatedFps)}")
        println("--------------------------------------------------")

        assertFalse("время инференса не должно быть нулевым — подозрение на баг замера", avgMs <= 0.0)
    }

    @Test
    fun hotSwapBetweenModelsDoesNotLeakOrCrash() {
        val strategies = listOf(
            YoloV8Strategy(inputSize = 640, fileName = "temp_model_640.tflite"),
            YoloV8Strategy(inputSize = 416, fileName = "temp_model_416.tflite"),
            YoloV8Strategy(inputSize = 224, fileName = "temp_model_224.tflite"),
            YoloV8Strategy(inputSize = 640, fileName = "temp_model_640.tflite"),
        )

        for (strategy in strategies) {
            engine.load(strategy)
            val detections = engine.run(testBitmap, CONFIDENCE_THRESHOLD)
            detections.forEach { d -> assertTrue(d.xMin < d.xMax && d.yMin < d.yMax) }
        }
    }
    @Test
    fun measureAverageInferenceTimeYolo26() {
        // Список конфигураций YOLO26 для последовательного тестирования производительности
        val modelsToBenchmark = listOf(
            YoloV26Strategy(inputSize = 640, fileName = "yolo26n_640.tflite"),
            YoloV26Strategy(inputSize = 416, fileName = "yolo26n_416.tflite"),
            YoloV26Strategy(inputSize = 224, fileName = "yolo26n_224.tflite")
        )

        println("\n=== НАЧАЛО СЕРИИ БЕНЧМАРКОВ: YOLO26 ===")

        for (strategy in modelsToBenchmark) {
            // Переключаем движок на нужную модель перед прогревом и замером
            engine.load(strategy)
            runBenchmarkForStrategy(strategy)
        }

        println("=== КОНЕЦ СЕРИИ БЕНЧМАРКОВ: YOLO26 ===\n")
    }
}