package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.usecase.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SystemViewModel(
    private val setMasterOutputMuteUseCase: SetMasterOutputMuteUseCase,
    private val setCameraPositionUseCase: SetCameraPositionUseCase,
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val observeMasterMuteUseCase: ObserveMasterMuteUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase
) : ViewModel() {

    val isMasterMuted: StateFlow<Boolean> = observeMasterMuteUseCase()
    val connectionStatus = observeConnectionStatusUseCase()

    fun toggleMasterMute() {
        viewModelScope.launch {
            setMasterOutputMuteUseCase(!isMasterMuted.value)
        }
    }

    fun setCameraPosition(channelId: Int) {
        viewModelScope.launch {
            setCameraPositionUseCase(channelId)
        }
    }

    fun changeDevice(ip: String, port: Int) {
        viewModelScope.launch {
            disconnectDeviceUseCase()
            connectToDeviceUseCase(ip, port)
        }
    }
}