package com.example.artrafficsign.di

import com.example.core_data.DataLayerApiImpl
import com.example.core_data.DynamicListsManagerImpl
import com.example.core_data.SettingsRepositoryImpl
import com.example.core_data.SignRepositoryImpl
import com.example.domain.api.CvLayerApi
import com.example.domain.api.DataLayerApi
import com.example.domain.api.IDynamicListsManager
import com.example.domain.api.ISettingsRepository
import com.example.domain.api.ISignRepository
import com.example.domain.api.VoiceLayerApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSignRepository(impl: SignRepositoryImpl): ISignRepository

    @Binds
    @Singleton
    abstract fun bindDynamicListsManager(impl: DynamicListsManagerImpl): IDynamicListsManager

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): ISettingsRepository

    @Binds
    @Singleton
    abstract fun bindDataLayerApi(impl: DataLayerApiImpl): DataLayerApi

    companion object {
        /**
         * Provides null for CvLayerApi as the module is not yet implemented.
         * AppViewModel handles this nullable dependency.
         */
        @Provides
        fun provideCvLayerApi(): CvLayerApi? = null
    }
}
