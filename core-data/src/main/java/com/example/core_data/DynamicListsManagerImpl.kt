package com.example.core_data

import com.example.domain.api.IDynamicListsManager
import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicListsManagerImpl : IDynamicListsManager {

    private val maxHistorySize: Int

    @Inject
    constructor() : this(50)

    constructor(maxHistorySize: Int) {
        this.maxHistorySize = maxHistorySize
    }

    private val _activeSigns = MutableStateFlow<List<ActiveSign>>(emptyList())
    private val _historySigns = MutableStateFlow<List<SignEntity>>(emptyList())

    override val activeSigns: StateFlow<List<ActiveSign>> = _activeSigns
    override val historySigns: StateFlow<List<SignEntity>> = _historySigns

    override suspend fun updateActiveSigns(signs: List<ActiveSign>) = withContext(Dispatchers.Default) {
        _activeSigns.value = signs
    }

    override suspend fun recordRecognizedSign(sign: SignEntity) = withContext(Dispatchers.Default) {
        val current = _historySigns.value.toMutableList()
        if (current.isNotEmpty() && current.first().id == sign.id) {
            return@withContext
        }
        current.add(0, sign)
        if (current.size > maxHistorySize) {
            current.removeAt(current.lastIndex)
        }
        _historySigns.value = current
    }

    override fun clear() {
        _activeSigns.value = emptyList()
        _historySigns.value = emptyList()
    }
}
