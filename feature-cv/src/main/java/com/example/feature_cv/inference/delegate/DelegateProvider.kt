package com.example.feature_cv.inference.delegate

/**
 * Подбирает лучший доступный TFLite-делегат для устройства пользователя.
 *
 * Порядок фолбэка: GPU delegate -> NNAPI -> CPU (XNNPACK).
 * GPU delegate поддерживается не на всех устройствах — обязательно проверять
 * через CompatibilityList().isDelegateSupportedOnThisDevice перед использованием,
 * иначе Interpreter может упасть или работать медленнее CPU.
 */
class DelegateProvider /* @Inject constructor(
    @ApplicationContext private val context: Context
) */ {

    /**
     * @return сконфигурированные Interpreter.Options с лучшим доступным делегатом.
     */
    fun createOptions(): Any /* Interpreter.Options */ {
        // TODO: 1. проверить CompatibilityList().isDelegateSupportedOnThisDevice(context) для GPU
        // TODO: 2. если GPU недоступен -> попробовать NNAPI
        // TODO: 3. если ничего нет -> CPU + setUseXNNPACK(true) + setNumThreads(...)
        TODO("не реализовано")
    }
}
