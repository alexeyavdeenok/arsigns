package com.example.feature_cv.di

/**
 * Именованные диспетчеры/executor'ы, чтобы явно разделять потоки:
 * - InferenceDispatcher: единственный поток для Interpreter.run() (не потокобезопасен)
 * - CameraDispatcher: поток для ImageAnalysis.Analyzer (не главный поток)
 *
 * Явное разделение — чтобы случайно не дёрнуть interpreter параллельно из двух мест.
 */
// @Module
// @InstallIn(SingletonComponent::class)
// object DispatcherModule {
//
//     TODO: @InferenceDispatcher — quality: один поток (Dispatchers.Default.limitedParallelism(1)
//           или отдельный single-thread Executor)
//     @Provides
//     @InferenceDispatcher
//     fun provideInferenceDispatcher(): CoroutineDispatcher { TODO() }
//
//     TODO: @CameraDispatcher — Executor для ImageAnalysis.setAnalyzer
//     @Provides
//     @CameraDispatcher
//     fun provideCameraExecutor(): Executor { TODO() }
// }

// TODO: @Qualifier annotation class InferenceDispatcher
// TODO: @Qualifier annotation class CameraDispatcher
