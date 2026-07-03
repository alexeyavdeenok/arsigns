package com.example.artrafficsign.di

import com.example.core_data.repository.DynamicListsManagerImpl
import com.example.core_data.repository.SettingsRepositoryImpl
import com.example.core_data.repository.SignRepositoryImpl
import com.example.domain.repository.IDynamicListsManager
import com.example.domain.repository.ISettingsRepository
import com.example.domain.repository.ISignRepository
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

    companion object {
        @Provides
        fun provideCvLayerApi(): com.example.domain.api.CvLayerApi? = null
    }
}
