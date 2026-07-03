package com.example.feature_cv.camera

/**
 * Единственная точка биндинга камеры к жизненному циклу.
 *
 * ВАЖНО: CameraX требует, чтобы Preview и ImageAnalysis биндились ОДНИМ вызовом
 * bindToLifecycle(...). Повторный вызов bindToLifecycle отвязывает предыдущие use-case'ы.
 * Поэтому CameraController — единственный владелец камеры в модуле,
 * а CameraPreview (UI) и FrameAnalyzer (CV) получают доступ только через него.
 *
 * Жизненный цикл:
 * - bind() вызывается один раз, когда экран с камерой становится видимым
 * - attachSurface() дёргается из Composable каждый раз, когда PreviewView готов
 * - startAnalysis()/stopAnalysis() управляют ТОЛЬКО ImageAnalysis, не трогая Preview
 *
 * @see CameraPreview публичный Composable, дёргающий attachSurface()
 * @see FrameAnalyzer анализатор, который сюда прицепляется через startAnalysis()
 */
class CameraController /* @Inject constructor(
    @ApplicationContext private val context: Context
) */ {

    // TODO: ProcessCameraProvider — получить через ProcessCameraProvider.getInstance(context)
    // TODO: Preview use case (CameraX)
    // TODO: ImageAnalysis use case, backpressure = STRATEGY_KEEP_ONLY_LATEST

    /**
     * Биндит Preview + ImageAnalysis к переданному LifecycleOwner.
     * Вызывается один раз при инициализации экрана с камерой.
     */
    fun bind(/* lifecycleOwner: LifecycleOwner */) {
        // TODO: cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
    }

    /**
     * Подключает SurfaceProvider от PreviewView к текущему Preview use-case.
     * Можно вызывать многократно (например при пересоздании Composable) без повторного bind().
     */
    fun attachSurface(/* surfaceProvider: Preview.SurfaceProvider */) {
        // TODO: preview.setSurfaceProvider(surfaceProvider)
    }

    /**
     * Включить анализ кадров (подключить analyzer к ImageAnalysis).
     * Preview продолжает работать независимо.
     */
    fun startAnalysis(/* analyzer: ImageAnalysis.Analyzer, executor: Executor */) {
        // TODO: imageAnalysis.setAnalyzer(executor, analyzer)
    }

    /**
     * Выключить анализ кадров. НЕ трогает bindToLifecycle и Preview.
     */
    fun stopAnalysis() {
        // TODO: imageAnalysis.clearAnalyzer()
    }

    /**
     * Полностью отвязать камеру (например при уничтожении экрана).
     */
    fun unbind() {
        // TODO: cameraProvider.unbindAll()
    }
}
