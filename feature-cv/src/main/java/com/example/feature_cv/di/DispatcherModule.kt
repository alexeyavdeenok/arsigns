package com.example.feature_cv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Единственный поток под все вызовы Interpreter (load/run/close) — Interpreter не
 * потокобезопасен, все обращения к нему обязаны идти строго последовательно с
 * одного и того же потока (см. обсуждение потокобезопасности TFLite Interpreter).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InferenceDispatcher

/** Поток для ImageAnalysis.Analyzer (CameraX) — не главный поток, отдельный от инференса. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CameraDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    @InferenceDispatcher
    fun provideInferenceDispatcher(): CoroutineDispatcher =
    // Именно ОДИН поток (не пул) — гарантия, что load()/run()/close() физически
    // не могут пересечься по времени, даже если их инициировали из разных мест
        // (CameraX callback и Flow-подписка на смену модели в ModelManager).
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "InferenceThread")
        }.asCoroutineDispatcher()

    @Provides
    @Singleton
    @CameraDispatcher
    fun provideCameraExecutor(): Executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "CameraAnalysisThread")
        }
}