package com.example.domain.api

import com.example.domain.model.AppSettings
import com.example.domain.model.YoloModelType
import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {
    val settingsFlow: StateFlow<AppSettings>

    suspend fun updateConfidenceThreshold(threshold: Float)

    suspend fun updateSelectedModel(modelType: YoloModelType)

    suspend fun toggleTts(enabled: Boolean)

    suspend fun setSettings(settings: AppSettings)
}
