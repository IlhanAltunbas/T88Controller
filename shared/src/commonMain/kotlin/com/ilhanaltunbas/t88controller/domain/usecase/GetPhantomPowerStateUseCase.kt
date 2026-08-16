package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetPhantomPowerStateUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channel: Int): Boolean {
        return repository.getPhantomPowerState(channel)
    }
}