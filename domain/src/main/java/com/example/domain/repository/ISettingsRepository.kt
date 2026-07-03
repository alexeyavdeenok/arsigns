package com.example.domain.repository

import com.example.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    val settingsFlow: Flow<AppSettings>
    fun updateConfidenceThreshold(value: Float)
    fun updateModelPath(path: String)
    fun toggleTts(enabled: Boolean)
}
