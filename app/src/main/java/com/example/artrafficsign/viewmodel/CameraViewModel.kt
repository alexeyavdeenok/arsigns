package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.api.IDynamicListsManager
import com.example.domain.api.ISettingsRepository
import com.example.domain.api.ISignRepository
import com.example.domain.api.VoiceLayerApi
import com.example.domain.model.ActiveSign
import com.example.domain.model.DetectedSign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val signRepository: ISignRepository,
    private val dynamicListsManager: IDynamicListsManager,
    private val settingsRepository: ISettingsRepository,
    private val voiceLayerApi: VoiceLayerApi
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.settingsFlow.value)

    init {
        viewModelScope.launch {
            signRepository.preloadCache()
        }
    }

    fun processDetectedSigns(detectedSigns: List<DetectedSign>) {
        viewModelScope.launch {
            val threshold = settingsRepository.settingsFlow.value.yoloConfidenceThreshold
            val filtered = detectedSigns.filter { it.confidence >= threshold }
            val settings = settingsRepository.settingsFlow.value
            dynamicListsManager.updateActiveSigns(filtered.map { sign ->
                ActiveSign(
                    signId = sign.id,
                    confidence = sign.confidence,
                    xMin = sign.xMin,
                    yMin = sign.yMin,
                    xMax = sign.xMax,
                    yMax = sign.yMax
                )
            })

            filtered.forEach { detected ->
                val sign = signRepository.getCachedSignById(detected.id)
                    ?: signRepository.getSignByYoloClassIndex(detected.yoloClassIndex)
                if (sign != null) {
                    dynamicListsManager.recordRecognizedSign(sign)
                    if (settings.isVoiceAlertsEnabled) {
                        voiceLayerApi.speak(sign.ttsTitle)
                    }
                }
            }
        }
    }

    fun onSignClicked(signId: Int) {
        viewModelScope.launch {
            val sign = signRepository.getSignById(signId)
            if (sign != null) {
                // The UI layer can observe this through navigation state in a future integration.
            }
        }
    }
}
