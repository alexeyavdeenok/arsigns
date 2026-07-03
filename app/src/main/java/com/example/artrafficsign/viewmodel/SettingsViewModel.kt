package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.api.ISettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.YoloModelType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings()
    )

    suspend fun updateConfidenceThreshold(threshold: Float) {
        settingsRepository.updateConfidenceThreshold(threshold)
    }

    suspend fun toggleTts(enabled: Boolean) {
        settingsRepository.toggleTts(enabled)
    }

    suspend fun updateSelectedModel(modelType: YoloModelType) {
        settingsRepository.updateSelectedModel(modelType)
    }
}
