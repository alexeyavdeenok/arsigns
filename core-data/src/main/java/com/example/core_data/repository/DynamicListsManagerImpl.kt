package com.example.core_data.repository

import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import com.example.domain.repository.IDynamicListsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicListsManagerImpl @Inject constructor() : IDynamicListsManager {

    private val _activeSigns = MutableStateFlow<List<ActiveSign>>(emptyList())
    override val activeSigns: StateFlow<List<ActiveSign>> = _activeSigns.asStateFlow()

    private val _historySigns = MutableStateFlow<List<SignEntity>>(emptyList())
    override val historySigns: StateFlow<List<SignEntity>> = _historySigns.asStateFlow()

    private var modelBusy: Boolean = false

    override fun updateActiveSigns(signs: List<ActiveSign>) {
        _activeSigns.value = signs
    }

    override fun setModelBusy(busy: Boolean) {
        modelBusy = busy
    }

    override fun isModelBusy(): Boolean = modelBusy

    /**
     * Метод для записи распознанного знака в историю.
     * Хотя его нет в интерфейсе IDynamicListsManager из диаграммы,
     * он нужен для функционирования логики истории.
     */
    fun recordRecognizedSign(sign: SignEntity) {
        val currentHistory = _historySigns.value.toMutableList()
        if (currentHistory.none { it.id == sign.id }) {
            currentHistory.add(0, sign)
            _historySigns.value = currentHistory
        }
    }
}
