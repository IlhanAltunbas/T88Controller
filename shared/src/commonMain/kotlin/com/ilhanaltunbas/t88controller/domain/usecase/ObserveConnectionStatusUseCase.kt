package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveConnectionStatusUseCase(private val repository: AudioMatrixRepository) {
    operator fun invoke(): StateFlow<ConnectionStatus> {
        return repository.getConnectionStatus()
    }
}