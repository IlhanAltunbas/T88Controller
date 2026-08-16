package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.usecase.ConnectToDeviceUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.DisconnectDeviceUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveConnectionStatusUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val connectToDeviceUseCase: ConnectToDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = observeConnectionStatusUseCase()

    fun connectToDevice(ip: String, port: Int) {
        // SENİN YAZDIĞIN HARİKA KONTROL: Çift tıklamayı engelle
        if (connectionStatus.value == ConnectionStatus.CONNECTING ||
            connectionStatus.value == ConnectionStatus.CONNECTED) {
            return
        }

        viewModelScope.launch {
            connectToDeviceUseCase(ip = ip, port = port)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectDeviceUseCase()
        }
    }
}