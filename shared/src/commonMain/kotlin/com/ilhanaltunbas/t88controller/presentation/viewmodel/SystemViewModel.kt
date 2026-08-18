package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.repository.SettingsRepository
import com.ilhanaltunbas.t88controller.domain.usecase.*
import com.ilhanaltunbas.t88controller.domain.model.SavedDevice
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SystemViewModel(
    private val setMasterOutputMuteUseCase: SetMasterOutputMuteUseCase,
    private val setCameraPositionUseCase: SetCameraPositionUseCase,
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val observeMasterMuteUseCase: ObserveMasterMuteUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val syncDeviceDataUseCase: SyncDeviceDataUseCase,
    private val observeCameraPositionUseCase: ObserveCameraPositionUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isMasterMuted: StateFlow<Boolean> = observeMasterMuteUseCase()
    val connectionStatus = observeConnectionStatusUseCase()
    val lastCameraPosition: StateFlow<Int> = observeCameraPositionUseCase()

    val currentIp = settingsRepository.getLastIp()
    val currentPort = settingsRepository.getLastPort().toString()

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
        settingsRepository.saveLastIp(ip)
        settingsRepository.saveLastPort(port)
        settingsRepository.saveDevice(SavedDevice("Cihaz ($ip)", ip, port))
        
        viewModelScope.launch {
            disconnectDeviceUseCase()
            val success = connectToDeviceUseCase(ip, port)
            if (success) {
                syncDeviceDataUseCase()
            }
        }
    }
}