package com.example.feature_cv

import com.example.domain.api.CvLayerApi
import com.example.domain.model.DetectedSign
import com.example.domain.repository.ISettingsRepository
import com.example.feature_cv.camera.CameraController
import com.example.feature_cv.camera.FrameAnalyzer
import com.example.feature_cv.di.CameraDispatcher
import com.example.feature_cv.di.InferenceDispatcher
import com.example.feature_cv.inference.InferenceEngine
import com.example.feature_cv.settings.ModelManager
import com.example.feature_cv.tracker.SignTracker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Точка склейки всего модуля. Единственная реализация domain.api.CvLayerApi.
 * :app работает только с этим контрактом, ничего не зная про CameraX/TFLite внутри.
 */
@Singleton
class CvLayerApiImpl @Inject constructor(
    private val cameraController: CameraController,
    private val inferenceEngine: InferenceEngine,
    private val tracker: SignTracker,
    private val modelManager: ModelManager,
    private val settingsRepository: ISettingsRepository,
    @InferenceDispatcher private val inferenceDispatcher: CoroutineDispatcher,
    @CameraDispatcher private val cameraExecutor: Executor
) : CvLayerApi {

    private val _liveDetectedSigns = MutableStateFlow<List<DetectedSign>>(emptyList())
    override val liveDetectedSigns: StateFlow<List<DetectedSign>> = _liveDetectedSigns.asStateFlow()

    /**
     * Собственный долгоживущий scope на InferenceDispatcher — сюда же ModelManager
     * подписывает смену модели. Раз это тот же диспетчер, на котором FrameAnalyzer
     * блокируется на время run() (см. FrameAnalyzer), хот-свап модели физически не
     * может пересечься по времени с обработкой кадра — оба ждут своей очереди на
     * одном и том же потоке.
     */
    private val engineScope = CoroutineScope(SupervisorJob() + inferenceDispatcher)

    private val frameAnalyzer = FrameAnalyzer(
        inferenceEngine = inferenceEngine,
        tracker = tracker,
        inferenceDispatcher = inferenceDispatcher,
        // Базовый "пол" уверенности — отсекает явный шум ещё до трекера.
        // НЕ пользовательский порог (yoloConfidenceThreshold) — тот применяется
        // в :app при отрисовке, а не здесь (см. обсуждение архитектуры фильтрации).
        confidenceThresholdProvider = { BASE_CONFIDENCE_FLOOR },
        onResult = { signs -> _liveDetectedSigns.value = signs }
    )

    init {
        modelManager.observeSettingsChanges(settingsRepository.settingsFlow, engineScope)
    }

    override fun startDetection() {
        cameraController.startAnalysis(frameAnalyzer, cameraExecutor)
    }

    override fun stopDetection() {
        cameraController.stopAnalysis()
    }

    companion object {
        private const val BASE_CONFIDENCE_FLOOR = 0.1f
    }
}