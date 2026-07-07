package com.example.feature_cv.camera

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Compose-функции не могут получить зависимость через @Inject напрямую (в отличие от
 * ViewModel/Activity/Fragment) — EntryPoint это стандартный способ Hilt достать
 * @Singleton-зависимость в месте, где обычная инъекция недоступна.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CameraControllerEntryPoint {
    fun cameraController(): CameraController
}

internal fun resolveCameraController(context: android.content.Context): CameraController =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        CameraControllerEntryPoint::class.java
    ).cameraController()