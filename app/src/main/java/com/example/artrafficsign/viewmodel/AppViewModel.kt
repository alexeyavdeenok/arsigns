package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ActiveSign
import com.example.domain.model.AppSettings
import com.example.domain.model.SignEntity
import com.example.domain.repository.IDynamicListsManager
import com.example.domain.repository.ISettingsRepository
import com.example.domain.repository.ISignRepository
import com.example.domain.repository.ITtsManager
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
    private val ttsManager: ITtsManager
) : ViewModel() {

    private val _allSigns = MutableStateFlow<List<SignEntity>>(emptyList())
    val allSigns: StateFlow<List<SignEntity>> = _allSigns.asStateFlow()

    val activeSigns: StateFlow<List<ActiveSign>> = dynamicListsManager.activeSigns
    val historySigns: StateFlow<List<SignEntity>> = dynamicListsManager.historySigns

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings()
    )

    private val _selectedSign = MutableStateFlow<SignEntity?>(null)
    val selectedSign: StateFlow<SignEntity?> = _selectedSign.asStateFlow()

    init {
        viewModelScope.launch {
            signRepository.preloadCache()
            // We need a way to get all signs if needed for a catalog, 
            // but for now we'll just preload.
        }
    }

    fun loadSign(signId: Int) {
        viewModelScope.launch {
            _selectedSign.value = signRepository.getSignById(signId)
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }
}
