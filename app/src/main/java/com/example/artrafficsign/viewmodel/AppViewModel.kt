package com.example.artrafficsign.viewmodel

import androidx.annotation.Nullable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.api.CvLayerApi
import com.example.domain.api.IDynamicListsManager
import com.example.domain.api.ISettingsRepository
import com.example.domain.api.ISignRepository
import com.example.domain.api.VoiceLayerApi
import com.example.domain.model.ActiveSign
import com.example.domain.model.AppSettings
import com.example.domain.model.DetectedSign
import com.example.domain.model.SignEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val signRepository: ISignRepository,
    private val dynamicListsManager: IDynamicListsManager,
    private val settingsRepository: ISettingsRepository,
    @Nullable private val cvLayerApi: CvLayerApi?,
    @Nullable private val voiceLayerApi: VoiceLayerApi?
) : ViewModel() {

    private val _allSigns = MutableStateFlow<List<SignEntity>>(emptyList())
    val allSigns: StateFlow<List<SignEntity>> = _allSigns.asStateFlow()

    val activeSigns: StateFlow<List<ActiveSign>> = dynamicListsManager.activeSigns
    val historySigns: StateFlow<List<SignEntity>> = dynamicListsManager.historySigns

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        settingsRepository.settingsFlow.value
    )

    private val _selectedSign = MutableStateFlow<SignEntity?>(null)
    val selectedSign: StateFlow<SignEntity?> = _selectedSign.asStateFlow()

    val isCvAvailable = MutableStateFlow(cvLayerApi != null)
    val isVoiceAvailable = MutableStateFlow(voiceLayerApi != null)
    val liveDetectedSigns: StateFlow<List<DetectedSign>> =
        cvLayerApi?.liveDetectedSigns ?: MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            signRepository.preloadCache()
            _allSigns.value = signRepository.getAllSigns()
            _selectedSign.value = null
        }
    }

    fun loadSign(signId: Int) {
        viewModelScope.launch {
            _selectedSign.value = signRepository.getSignById(signId)
        }
    }

    fun speakText(text: String) {
        voiceLayerApi?.speak(text)
    }

    fun startDetection() {
        cvLayerApi?.startDetection()
    }

    fun stopDetection() {
        cvLayerApi?.stopDetection()
    }
}
