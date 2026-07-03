package com.example.artrafficsign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ActiveSign
import com.example.domain.model.SignEntity
import com.example.domain.repository.IDynamicListsManager
import com.example.domain.repository.ISignRepository
import com.example.domain.repository.ITtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val signRepository: ISignRepository,
    private val dynamicListsManager: IDynamicListsManager,
    private val ttsManager: ITtsManager
) : ViewModel() {

    val uiState: StateFlow<List<ActiveSign>> = dynamicListsManager.activeSigns
    val historyState: StateFlow<List<SignEntity>> = dynamicListsManager.historySigns

    init {
        viewModelScope.launch {
            signRepository.preloadCache()
        }
    }

    fun onSignClicked(signId: Int) {
        viewModelScope.launch {
            val sign = signRepository.getSignById(signId)
            sign?.let {
                ttsManager.speak(it.ttsTitle)
            }
        }
    }
    
    fun updateDetections(signs: List<ActiveSign>) {
        dynamicListsManager.updateActiveSigns(signs)
    }
}
