package com.example.core_data

import com.example.domain.api.DataLayerApi
import com.example.domain.model.AppSettings
import com.example.domain.model.SignMetadata
import com.example.domain.model.YoloModelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataLayerApiImpl @Inject constructor(
    private val settingsRepository: SettingsRepositoryImpl,
    private val signRepository: SignRepositoryImpl
) : DataLayerApi {

    override val appSettings: Flow<AppSettings> = settingsRepository.settingsFlow

    override suspend fun updateConfidenceThreshold(threshold: Float) {
        settingsRepository.updateConfidenceThreshold(threshold)
    }

    override suspend fun updateSelectedModel(modelType: YoloModelType) {
        settingsRepository.updateSelectedModel(modelType)
    }

    override suspend fun updateVoiceAlertsEnabled(isEnabled: Boolean) {
        settingsRepository.toggleTts(isEnabled)
    }

    override suspend fun getSignMetadataByYoloIndex(yoloClassIndex: String): SignMetadata? {
        val sign = signRepository.getSignByYoloClassIndex(yoloClassIndex) ?: return null
        return SignMetadata(
            yoloClassIndex = sign.pddCode,
            gostSignNumber = sign.pddCode,
            title = sign.title,
            voiceText = sign.ttsTitle,
            description = sign.description,
            photoPath = sign.svgPath
        )
    }

    override suspend fun getAllSignsMetadata(): List<SignMetadata> {
        return signRepository.getAllSigns().map { sign ->
            SignMetadata(
                yoloClassIndex = sign.pddCode,
                gostSignNumber = sign.pddCode,
                title = sign.title,
                voiceText = sign.ttsTitle,
                description = sign.description,
                photoPath = sign.svgPath
            )
        }
    }
}
