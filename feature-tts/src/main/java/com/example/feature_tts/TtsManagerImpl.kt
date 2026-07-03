package com.example.feature_tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.domain.api.VoiceLayerApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManagerImpl @Inject constructor(
    @ApplicationContext context: Context
) : VoiceLayerApi, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arsigns")
    }

    override fun stopSpeaking() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.shutdown()
    }
}
