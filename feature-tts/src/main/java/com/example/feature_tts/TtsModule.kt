package com.example.feature_tts

import com.example.domain.api.VoiceLayerApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    @Binds
    @Singleton
    abstract fun bindVoiceLayerApi(impl: TtsManagerImpl): VoiceLayerApi
}
