package com.example.feature_cv.camera

/**
 * Публичный Composable — единственная UI-точка входа в модуль камеры.
 * :app вставляет его напрямую в свою композицию (:app зависит от :feature-cv напрямую,
 * поэтому прокидывать это через :domain не нужно — см. обсуждение архитектуры).
 *
 * Использование в :app:
 *   CameraPreview(modifier = Modifier.fillMaxSize())
 *
 * Рамки поверх этого превью рисует :app самостоятельно, подписавшись на
 * CvLayerApi.liveDetectedSigns (domain-интерфейс, с этим Composable не связан).
 *
 * Важно: реальные байты кадров сюда не поступают и через этот Composable не идут —
 * передаётся только ссылка на SurfaceProvider, дальше CameraX рисует кадры
 * по аппаратному пути напрямую в Surface.
 */
// @Composable
// fun CameraPreview(modifier: Modifier = Modifier) {
//     TODO: получить CameraController через hiltViewModel() или EntryPointAccessors
//     TODO: AndroidView(factory = { PreviewView(it) }, update = { cameraController.attachSurface(it.surfaceProvider) })
//     TODO: DisposableEffect(Unit) { cameraController.bind(lifecycleOwner); onDispose { cameraController.unbind() } }
// }
