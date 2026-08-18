package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.model.SavedDevice
import com.ilhanaltunbas.t88controller.domain.usecase.ConnectToDeviceUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.DisconnectDeviceUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveConnectionStatusUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.SyncDeviceDataUseCase
import com.ilhanaltunbas.t88controller.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val syncDeviceDataUseCase: SyncDeviceDataUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = observeConnectionStatusUseCase()

    private val _savedDevices = MutableStateFlow<List<SavedDevice>>(emptyList())
    val savedDevices: StateFlow<List<SavedDevice>> = _savedDevices.asStateFlow()

    val initialIp = settingsRepository.getLastIp()
    val initialPort = settingsRepository.getLastPort().toString()

    init {
        loadSavedDevices()
    }

    fun loadSavedDevices() {
        _savedDevices.value = settingsRepository.getSavedDevices()
    }

    fun connectToDevice(ip: String, port: Int, deviceName: String = "") {
        if (connectionStatus.value == ConnectionStatus.CONNECTING ||
            connectionStatus.value == ConnectionStatus.CONNECTED) {
            return
        }

        viewModelScope.launch {
            val success = connectToDeviceUseCase(ip = ip, port = port)
            if (success) {
                // Başarılıysa ayarlara kaydet
                settingsRepository.saveLastIp(ip)
                settingsRepository.saveLastPort(port)
                
                // Cihaz listesine ekle
                val name = if (deviceName.isBlank()) "Cihaz ($ip)" else deviceName
                settingsRepository.saveDevice(SavedDevice(name, ip, port))
                loadSavedDevices()

                syncDeviceDataUseCase()
            }
        }
    }

    fun deleteSavedDevice(device: SavedDevice) {
        settingsRepository.deleteDevice(device.ip, device.port)
        loadSavedDevices()
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectDeviceUseCase()
        }
    }
}