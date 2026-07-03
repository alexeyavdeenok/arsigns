package com.example.feature_cv.inference

import com.example.feature_cv.inference.delegate.DelegateProvider
import com.example.feature_cv.inference.model.RawDetection

/**
 * Обёртка над TFLite Interpreter. Единая точка запуска модели.
 *
 * ВАЖНО: Interpreter НЕ потокобезопасен. Все вызовы run() должны идти
 * последовательно с одного диспетчера (см. di/DispatcherModule -> InferenceDispatcher).
 *
 * Хот-свап модели: при смене модели в настройках ModelManager вызывает load(),
 * который обязан закрыть старый Interpreter (interpreter.close()) перед созданием нового,
 * иначе будет утечка нативной памяти.
 *
 * @see ModelManager управляет тем, когда пересоздавать модель
 * @see ModelStrategy как препроцессить/постпроцессить под конкретную модель
 */
class InferenceEngine /* @Inject constructor(
    private val delegateProvider: DelegateProvider,
    @ApplicationContext private val context: Context
) */ {

    // TODO: private var interpreter: Interpreter? = null
    // TODO: private var currentStrategy: ModelStrategy? = null

    /**
     * (Пере)загрузить модель из assets по заданной стратегии.
     * Закрывает предыдущий Interpreter, если был.
     */
    fun load(strategy: ModelStrategy) {
        // TODO: interpreter?.close()
        // TODO: interpreter = Interpreter(loadModelFile(strategy.fileName), delegateProvider.createOptions())
        // TODO: currentStrategy = strategy
    }

    /**
     * Прогнать один кадр через модель.
     * Вызывается строго с одного и того же диспетчера (не параллельно).
     *
     * @return список сырых детекций (после NMS, до трекера)
     */
    fun run(/* bitmap: Bitmap, */ confidenceThreshold: Float): List<RawDetection> {
        // TODO: val input = currentStrategy!!.preprocess(bitmap)
        // TODO: interpreter!!.run(input, output)
        // TODO: return currentStrategy!!.postprocess(output, confidenceThreshold)
        TODO("не реализовано")
    }

    fun close() {
        // TODO: interpreter?.close()
    }
}
