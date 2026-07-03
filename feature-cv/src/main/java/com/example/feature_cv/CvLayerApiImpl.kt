package com.example.feature_cv

/**
 * Точка склейки всего модуля. Единственная реализация com.example.domain.api.CvLayerApi.
 * :app работает только с этим контрактом, ничего не зная про CameraX/TFLite внутри.
 *
 * Ответственность:
 * - держать MutableStateFlow, который отдаётся наружу как liveDetectedSigns
 * - startDetection()/stopDetection() -> делегировать в CameraController.startAnalysis()/stopAnalysis()
 * - запустить ModelManager.observeSettingsChanges() при инициализации
 */
class CvLayerApiImpl /* @Inject constructor(
    private val cameraController: com.example.feature_cv.camera.CameraController,
    private val frameAnalyzer: com.example.feature_cv.camera.FrameAnalyzer,
    private val modelManager: com.example.feature_cv.settings.ModelManager,
    @com.example.feature_cv.di.CameraDispatcher private val cameraDispatcher: java.util.concurrent.Executor
) : com.example.domain.api.CvLayerApi */ {

    // TODO: private val _liveDetectedSigns = MutableStateFlow<List<DetectedSign>>(emptyList())
    // override val liveDetectedSigns: StateFlow<List<DetectedSign>> get() = _liveDetectedSigns

    // init { modelManager.observeSettingsChanges(scope) }

    // override fun startDetection() { cameraController.startAnalysis(frameAnalyzer, cameraDispatcher) }
    // override fun stopDetection() { cameraController.stopAnalysis() }
}
