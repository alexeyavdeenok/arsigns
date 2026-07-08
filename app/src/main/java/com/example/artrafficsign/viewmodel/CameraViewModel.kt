package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.api.CvLayerApi
import com.example.domain.api.VoiceLayerApi
import com.example.domain.model.ActiveSign
import com.example.domain.model.FrameSize
import com.example.domain.model.SignEntity
import com.example.domain.repository.IDynamicListsManager
import com.example.domain.repository.ISettingsRepository
import com.example.domain.repository.ISignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    /**
     * Пользовательский порог уверенности применяется ЗДЕСЬ, а не в :feature-cv —
     * это UX-настройка отображения, должна применяться мгновенно при движении слайдера,
     * без повторного инференса. :feature-cv/трекер видят ВСЕ детекции выше базового
     * "пола" (см. BASE_CONFIDENCE_FLOOR в CvLayerApiImpl) независимо от этого порога —
     * иначе TTL трекера ломался бы при каждом движении слайдера пользователем.
     */
    val uiState: StateFlow<List<ActiveSign>> = combine(
        dynamicListsManager.activeSigns,
        settingsRepository.settingsFlow
    ) { activeSigns, settings ->
        activeSigns.filter { it.confidence >= settings.yoloConfidenceThreshold }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyState: StateFlow<List<SignEntity>> = dynamicListsManager.historySigns

    /** Размер кадра — нужен :app, чтобы корректно пересчитать нормализованные координаты рамок на экран. */
    val frameSize: StateFlow<FrameSize?> = cvLayerApi.frameSize

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