package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class ConnectToDeviceUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(ip: String, port: Int): Boolean {
        if (ip.isBlank() || port <= 0) return false
        return repository.connectToDevice(ip, port)
    }
}