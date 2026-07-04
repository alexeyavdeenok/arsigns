package com.example.artrafficsign.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        fun provideCvLayerApi(): com.example.domain.api.CvLayerApi? = null
    }
}
