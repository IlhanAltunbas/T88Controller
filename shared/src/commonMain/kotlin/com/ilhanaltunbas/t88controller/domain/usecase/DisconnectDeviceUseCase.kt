package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class DisconnectDeviceUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke() {
        repository.disconnectDevice()
    }
}