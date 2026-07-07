package com.example.feature_cv.di

import com.example.domain.api.CvLayerApi
import com.example.feature_cv.CvLayerApiImpl
import com.example.feature_cv.tracker.IouSignTracker
import com.example.feature_cv.tracker.SignTracker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-граф модуля feature-cv. Единственное место, где нужно поменять биндинг
 * при замене реализации (например SignTracker -> ByteTrack-подобный, когда/если
 * дойдут руки — см. обсуждение архитектуры трекера).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CvModule {

    @Binds
    abstract fun bindCvLayerApi(impl: CvLayerApiImpl): CvLayerApi

    companion object {
        /**
         * @Provides, а не @Binds — сознательно: IouSignTracker использует Kotlin
         * default-параметры (iouThreshold, maxLives), а автогенерируемый Hilt/Dagger
         * биндинг конструктора с default-аргументами — источник лишних сюрпризов.
         * Простой @Provides вызывает обычный Kotlin-конструктор напрямую, без вопросов.
         */
        @Provides
        @Singleton
        fun provideSignTracker(): SignTracker = IouSignTracker()
    }
}