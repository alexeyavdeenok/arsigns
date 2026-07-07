package com.example.feature_cv.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Публичный Composable — единственная UI-точка входа в модуль камеры.
 * :app вставляет его напрямую в свою композицию (:app зависит от :feature-cv напрямую,
 * поэтому прокидывать это через :domain не нужно — см. обсуждение архитектуры модулей).
 *
 * Использование в :app:
 *   CameraPreview(modifier = Modifier.fillMaxSize())
 *
 * Рамки поверх этого превью рисует :app самостоятельно, подписавшись на
 * IDynamicListsManager.activeSigns — этот Composable про рамки ничего не знает.
 *
 * Жизненный цикл камеры (bind/unbind) привязан к тому, находится ли ЭТОТ Composable
 * в композиции — свернули вкладку/экран с камерой -> DisposableEffect.onDispose
 * сработает -> CameraController.unbind() -> камера освобождена.
 */
@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { resolveCameraController(context) }

    DisposableEffect(lifecycleOwner) {
        cameraController.bind(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> PreviewView(ctx) },
        update = { previewView ->
            // Может вызываться многократно (рекомпозиция) — attachSurface() идемпотентен
            cameraController.attachSurface(previewView.surfaceProvider)
        }
    )
}