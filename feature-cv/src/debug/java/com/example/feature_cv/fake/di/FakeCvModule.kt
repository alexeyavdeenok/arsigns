package com.example.feature_cv.fake.di

import com.example.domain.api.CvLayerApi
import com.example.feature_cv.fake.FakeCvLayerApiImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Живёт ТОЛЬКО в src/debug — в release-сборку этот файл физически не попадает.
 * Единственная задача: подставить FakeCvLayerApiImpl в граф Hilt как реализацию CvLayerApi,
 * пока настоящий CvLayerApiImpl не готов.
 *
 * Когда реальный CvLayerApiImpl появится в src/main/.../di/CvModule.kt —
 * этот файл (и вся папка src/debug/.../fake) удаляется целиком, см. README.md рядом.
 */
/**
@Module
@InstallIn(SingletonComponent::class)
object FakeCvModule {

    @Provides
    @Singleton
    fun provideCvLayerApi(): CvLayerApi = FakeCvLayerApiImpl()
}
*/