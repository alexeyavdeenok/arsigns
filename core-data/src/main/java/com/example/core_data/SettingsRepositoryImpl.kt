package com.example.core_data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.api.ISettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.YoloModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : ISettingsRepository {
    private val dataStore = context.dataStore
    private val _settingsFlow = MutableStateFlow(AppSettings())
    private val confidenceKey = floatPreferencesKey("confidence_threshold")
    private val ttsKey = booleanPreferencesKey("tts_enabled")
    private val modelKey = stringPreferencesKey("selected_model")

    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    init {
        runBlocking {
            val preferences = dataStore.data.first()
            _settingsFlow.value = AppSettings(
                isVoiceAlertsEnabled = preferences[ttsKey] ?: true,
                yoloConfidenceThreshold = preferences[confidenceKey] ?: 0.5f,
                selectedModel = preferences[modelKey]?.let { modelName ->
                    YoloModelType.entries.firstOrNull { it.name == modelName } ?: YoloModelType.YOLO_V8_640
                } ?: YoloModelType.YOLO_V8_640
            )
        }
    }

    override suspend fun updateConfidenceThreshold(threshold: Float) {
        dataStore.edit { prefs -> prefs[confidenceKey] = threshold }
        _settingsFlow.update { it.copy(yoloConfidenceThreshold = threshold) }
    }

    override suspend fun updateSelectedModel(modelType: YoloModelType) {
        dataStore.edit { prefs -> prefs[modelKey] = modelType.name }
        _settingsFlow.update { it.copy(selectedModel = modelType) }
    }

    override suspend fun toggleTts(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[ttsKey] = enabled }
        _settingsFlow.update { it.copy(isVoiceAlertsEnabled = enabled) }
    }

    override suspend fun setSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[confidenceKey] = settings.yoloConfidenceThreshold
            prefs[ttsKey] = settings.isVoiceAlertsEnabled
            prefs[modelKey] = settings.selectedModel.name
        }
        _settingsFlow.value = settings
    }
}
