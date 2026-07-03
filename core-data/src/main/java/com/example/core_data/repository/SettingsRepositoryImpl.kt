package com.example.core_data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.repository.ISettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ISettingsRepository {

    private val dataStore = context.dataStore

    private val confidenceKey = floatPreferencesKey("confidence_threshold")
    private val modelPathKey = stringPreferencesKey("active_model_path")
    private val ttsKey = booleanPreferencesKey("is_tts_enabled")

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            confidenceThreshold = preferences[confidenceKey] ?: 0.5f,
            activeModelPath = preferences[modelPathKey] ?: "yolov8n.tflite",
            isTtsEnabled = preferences[ttsKey] ?: true
        )
    }

    override fun updateConfidenceThreshold(value: Float) {
        runBlocking {
            dataStore.edit { it[confidenceKey] = value }
        }
    }

    override fun updateModelPath(path: String) {
        runBlocking {
            dataStore.edit { it[modelPathKey] = path }
        }
    }

    override fun toggleTts(enabled: Boolean) {
        runBlocking {
            dataStore.edit { it[ttsKey] = enabled }
        }
    }
}
