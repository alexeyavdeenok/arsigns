package com.example.domain.repository

import com.example.domain.model.AppSettings
import com.example.domain.model.YoloModelType
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    val settingsFlow: Flow<AppSettings>
    fun updateConfidenceThreshold(value: Float)
    fun updateModel(type: YoloModelType)
    fun toggleTts(enabled: Boolean)
}
