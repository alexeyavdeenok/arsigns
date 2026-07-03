package com.example.domain.api

import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import kotlinx.coroutines.flow.StateFlow

interface IDynamicListsManager {
    val activeSigns: StateFlow<List<ActiveSign>>
    val historySigns: StateFlow<List<SignEntity>>

    suspend fun updateActiveSigns(signs: List<ActiveSign>)

    suspend fun recordRecognizedSign(sign: SignEntity)

    fun clear()
}
