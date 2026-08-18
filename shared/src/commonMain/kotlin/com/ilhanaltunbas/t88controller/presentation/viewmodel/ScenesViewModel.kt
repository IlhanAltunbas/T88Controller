package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveConnectionStatusUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveCurrentPresetUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.RecallSceneUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScenesViewModel(
    private val recallSceneUseCase: RecallSceneUseCase,
    private val observeCurrentPresetUseCase: ObserveCurrentPresetUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase
) : ViewModel() {

    private val _scenes = MutableStateFlow(List(30) { "Sahne ${it + 1}" })
    val scenes: StateFlow<List<String>> = _scenes.asStateFlow()

    val activePreset: StateFlow<Int> = observeCurrentPresetUseCase()
    val connectionStatus = observeConnectionStatusUseCase()

    fun recallScene(presetId: Int) {
        viewModelScope.launch {
            recallSceneUseCase(presetId)
        }
    }
}