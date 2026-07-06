package com.example.feature_tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.domain.api.VoiceLayerApi
import com.example.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManagerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsRepository: ISettingsRepository // Внедряем настройки
) : VoiceLayerApi, TextToSpeech.OnInitListener { // Переключаемся на правильный интерфейс

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isVoiceAlertsEnabled = true
    private var utteranceId = 0

    // Своя корутин-скоуп для прослушивания Flow настроек
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        tts = TextToSpeech(context, this)

        // Слушаем настройки. Если звук выключили — мгновенно затыкаем TTS
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                isVoiceAlertsEnabled = settings.isVoiceAlertsEnabled
                if (!isVoiceAlertsEnabled) {
                    stop()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
    }

    // Обычная озвучка новых знаков с камеры (встает в очередь)
    override fun speak(text: String) {
        if (!isReady || !isVoiceAlertsEnabled) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "arsigns-add-${utteranceId++}")
    }

    // Приоритетная озвучка по клику пользователя (сбивает текущую очередь)
    override fun speakPriority(ttsText: String) {
        if (!isReady || !isVoiceAlertsEnabled) return
        tts?.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, "arsigns-flush-${utteranceId++}")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
    }
}