package com.example.feature_cv.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единственная точка биндинга камеры к жизненному циклу.
 *
 * ВАЖНО: CameraX требует, чтобы Preview и ImageAnalysis биндились ОДНИМ вызовом
 * bindToLifecycle(...). Повторный вызов bindToLifecycle отвязывает предыдущие use-case'ы —
 * поэтому CameraController единственный, кто вообще вызывает bindToLifecycle в модуле.
 *
 * ProcessCameraProvider.getInstance() асинхронный (ListenableFuture) — attachSurface()/
 * startAnalysis() могут быть вызваны ДО того, как провайдер готов (например Composable
 * успел скомпоноваться раньше, чем прогрузился future). Поэтому оба хранят "отложенное"
 * состояние и применяются повторно сразу после готовности провайдера.
 */
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var pendingSurfaceProvider: Preview.SurfaceProvider? = null
    private var pendingAnalyzer: Pair<ImageAnalysis.Analyzer, Executor>? = null

    /**
     * Биндит Preview + ImageAnalysis к переданному LifecycleOwner.
     * Идемпотентен: повторный вызов с тем же lifecycleOwner ничего не ломает благодаря
     * cameraProvider.unbindAll() перед повторным bindToLifecycle.
     */
    fun bind(lifecycleOwner: LifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()

            val newPreview = Preview.Builder().build()
            val newImageAnalysis = ImageAnalysis.Builder()
                // Пока анализ занят текущим кадром — новые кадры не копятся в очереди,
                // остаётся только самый свежий. Это и есть "пропуск кадров, пока YOLO занята".
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                newPreview,
                newImageAnalysis
            )

            cameraProvider = provider
            preview = newPreview
            imageAnalysis = newImageAnalysis

            // Применяем то, что могло прийти раньше, чем провайдер стал готов
            pendingSurfaceProvider?.let { newPreview.setSurfaceProvider(it) }
            pendingAnalyzer?.let { (analyzer, executor) -> newImageAnalysis.setAnalyzer(executor, analyzer) }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Подключает SurfaceProvider от PreviewView к текущему Preview use-case.
     * Можно вызывать многократно без повторного bind().
     */
    fun attachSurface(surfaceProvider: Preview.SurfaceProvider) {
        pendingSurfaceProvider = surfaceProvider
        preview?.setSurfaceProvider(surfaceProvider)
    }

    /** Включить анализ кадров. Preview продолжает работать независимо. */
    fun startAnalysis(analyzer: ImageAnalysis.Analyzer, executor: Executor) {
        pendingAnalyzer = analyzer to executor
        imageAnalysis?.setAnalyzer(executor, analyzer)
    }

    /** Выключить анализ кадров. НЕ трогает bindToLifecycle и Preview. */
    fun stopAnalysis() {
        pendingAnalyzer = null
        imageAnalysis?.clearAnalyzer()
    }

    /** Полностью отвязать камеру (например при уничтожении экрана). */
    fun unbind() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        preview = null
        imageAnalysis = null
        pendingSurfaceProvider = null
        pendingAnalyzer = null
    }
}