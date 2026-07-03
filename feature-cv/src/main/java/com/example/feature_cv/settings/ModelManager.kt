package com.example.feature_cv.settings

/**
 * Слушает изменения AppSettings.selectedModel и управляет горячей заменой модели
 * в InferenceEngine без пересборки приложения.
 *
 * Поток обновления:
 * AppSettings (Flow, приходит извне модуля через :domain) -> ModelManager
 *   -> ModelStrategyFactory.create(newType) -> inferenceEngine.load(newStrategy)
 *
 * ВАЖНО: смена модели должна происходить на том же диспетчере, что и run(),
 * чтобы не пересоздать Interpreter посреди выполнения инференса.
 */
class ModelManager /* @Inject constructor(
    private val inferenceEngine: com.example.feature_cv.inference.InferenceEngine,
    private val strategyFactory: com.example.feature_cv.inference.ModelStrategyFactory
    // + источник Flow<AppSettings>, приходит из другого модуля через :domain
) */ {

    /**
     * Подписаться на изменения настроек и переинициализировать модель при смене.
     * Вызывается один раз при старте CvLayerApiImpl.
     */
    fun observeSettingsChanges(/* scope: CoroutineScope */) {
        // TODO: settingsFlow
        //     .map { it.selectedModel }
        //     .distinctUntilChanged()
        //     .onEach { type -> inferenceEngine.load(strategyFactory.create(type)) }
        //     .launchIn(scope)
    }
}
