package com.example.domain.repository

import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import kotlinx.coroutines.flow.StateFlow

interface IDynamicListsManager {
    val activeSigns: StateFlow<List<ActiveSign>>
    val historySigns: StateFlow<List<SignEntity>>
    fun updateActiveSigns(signs: List<ActiveSign>)
    fun setModelBusy(busy: Boolean)
    fun isModelBusy(): Boolean
}
