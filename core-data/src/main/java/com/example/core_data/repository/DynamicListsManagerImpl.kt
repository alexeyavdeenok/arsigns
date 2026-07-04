package com.example.core_data.repository

import com.example.core_data.di.ApplicationScope
import com.example.domain.api.CvLayerApi
import com.example.domain.model.ActiveSign
import com.example.domain.model.DetectedSign
import com.example.domain.model.SignEntity
import com.example.domain.repository.ISignRepository
import com.example.domain.repository.IDynamicListsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicListsManagerImpl @Inject constructor(
    private val cvLayerApi: CvLayerApi,
    private val signRepository: ISignRepository,
    @param:ApplicationScope private val scope: CoroutineScope
) : IDynamicListsManager {

    private val _activeSigns = MutableStateFlow<List<ActiveSign>>(emptyList())
    override val activeSigns: StateFlow<List<ActiveSign>> = _activeSigns.asStateFlow()

    private val _historySigns = MutableStateFlow<List<SignEntity>>(emptyList())
    override val historySigns: StateFlow<List<SignEntity>> = _historySigns.asStateFlow()

    init {
        scope.launch {
            // Прогреваем кэш репозитория ДО первой подписки на детекции,
            // иначе первые несколько кадров будут ходить в БД вхолостую (или ловить null).
            signRepository.preloadCache()

            cvLayerApi.liveDetectedSigns
                .onEach { detectedSigns -> _activeSigns.value = toActiveSigns(detectedSigns) }
                .launchIn(scope)
        }
    }

    /**
     * List<T>.map — inline-функция, поэтому suspend-вызовы внутри лямбды легальны
     * (всё разворачивается в текущей корутине без доп. диспетчеризации на каждый элемент).
     */
    private suspend fun toActiveSigns(detected: List<DetectedSign>): List<ActiveSign> =
        detected.mapNotNull { toActiveSign(it) }

    private suspend fun toActiveSign(detected: DetectedSign): ActiveSign? {
        val signEntity = signRepository.getSignById(detected.classId) ?: return null

        return ActiveSign(
            trackerId = detected.id,
            xMin = detected.xMin,
            yMin = detected.yMin,
            xMax = detected.xMax,
            yMax = detected.yMax,
            confidence = detected.confidence,
            classId = detected.classId,
            sign = signEntity
        )
    }
    override fun clearActiveSigns() {
        _activeSigns.value = emptyList()
        // _historySigns не трогаем вообще
    }
}