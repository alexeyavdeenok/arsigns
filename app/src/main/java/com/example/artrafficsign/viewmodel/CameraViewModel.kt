package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.api.CvLayerApi
import com.example.domain.api.VoiceLayerApi
import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import com.example.domain.repository.IDynamicListsManager
import com.example.domain.repository.ISettingsRepository
import com.example.domain.repository.ISignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cvLayerApi: CvLayerApi,
    private val signRepository: ISignRepository,
    private val dynamicListsManager: IDynamicListsManager,
    private val settingsRepository: ISettingsRepository,
    private val voiceLayerApi: VoiceLayerApi
) : ViewModel() {

    val uiState: StateFlow<List<ActiveSign>> = dynamicListsManager.activeSigns
    val historyState: StateFlow<List<SignEntity>> = dynamicListsManager.historySigns
    private var isVoiceAlertsEnabled: Boolean = true

    init {
        viewModelScope.launch {
            signRepository.preloadCache()
        }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                isVoiceAlertsEnabled = settings.isVoiceAlertsEnabled
                if (!settings.isVoiceAlertsEnabled) {
                    voiceLayerApi.stop()
                }
            }
        }

        viewModelScope.launch {
            var previousTrackIds = emptySet<Int>()
            uiState.collect { activeSigns ->
                val currentTrackIds = activeSigns.map { it.trackerId }.toSet()
                val newSign = activeSigns.firstOrNull { it.trackerId !in previousTrackIds }

                if (newSign != null && isVoiceAlertsEnabled) {
                    voiceLayerApi.speak(newSign.sign.ttsTitle)
                }

                previousTrackIds = currentTrackIds
            }
        }
    }

    fun startDetection() {
        cvLayerApi.startDetection()
    }

    fun stopDetection() {
        cvLayerApi.stopDetection()
        dynamicListsManager.clearActiveSigns()
        voiceLayerApi.stop()
    }

    override fun onCleared() {
        cvLayerApi.stopDetection()
        super.onCleared()
    }
}
