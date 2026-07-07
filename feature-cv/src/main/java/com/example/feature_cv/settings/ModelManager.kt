package com.example.feature_cv.settings

import com.example.domain.model.AppSettings
import com.example.feature_cv.inference.InferenceEngine
import com.example.feature_cv.inference.ModelStrategyFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Слушает изменения AppSettings.selectedModel и управляет горячей заменой модели
 * в InferenceEngine без пересборки приложения.
 *
 * ВАЖНО: settingsFlow должен собираться (launchIn) на том же диспетчере, что и
 * InferenceEngine.run() (InferenceDispatcher) — это гарантируется тем, что сюда
 * передаётся уже готовый CoroutineScope с нужным диспетчером снаружи (см. CvLayerApiImpl),
 * а не создаётся здесь самостоятельно на произвольном диспетчере.
 */
class ModelManager @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val strategyFactory: ModelStrategyFactory
) {

    /**
     * Подписаться на изменения настроек и переинициализировать модель при смене.
     * Вызывается один раз при старте CvLayerApiImpl.
     *
     * @param settingsFlow источник настроек из :domain (приходит извне модуля)
     * @param scope должен использовать InferenceDispatcher — тот же, что и InferenceEngine.run()
     */
    fun observeSettingsChanges(settingsFlow: Flow<AppSettings>, scope: CoroutineScope) {
        settingsFlow
            .map { it.selectedModel }
            .distinctUntilChanged()
            .onEach { type -> inferenceEngine.load(strategyFactory.create(type)) }
            .launchIn(scope)
    }
}