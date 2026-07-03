package com.example.feature_cv.inference

/**
 * Инструментальный тест-бенчмарк — нужен реальный девайс/эмулятор (androidTest, не test).
 * Модель кладётся в src/androidTest/assets, тест гоняет N прогонов на фиксированном
 * тестовом изображении и печатает среднее/p95 время инференса — это и есть замер FPS
 * без необходимости поднимать :app или :domain целиком.
 *
 * Опционально: обернуть interpreter.run() в Trace.beginSection("yolo_inference")/endSection()
 * и смотреть в Android Studio Profiler / Perfetto, что именно ест время —
 * препроцессинг, сам инференс модели или трекер.
 */
// @RunWith(AndroidJUnit4::class)
// class InferenceEngineBenchmarkTest {
//
//     TODO: @Before подготовить InferenceEngine с реальным .tflite из androidTest/assets
//     TODO: @Test warmUp — прогнать 5-10 кадров вхолостую (JIT/делегат "прогревается")
//     TODO: @Test measureAverageInferenceTimeGpu — 100 прогонов, посчитать среднее/p95, вывести FPS = 1000 / avgMs
//     TODO: @Test measureAverageInferenceTimeCpuFallback — то же самое, но форсируя CPU-делегат для сравнения
// }
