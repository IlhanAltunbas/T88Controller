package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetPhantomPowerUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelId: Int, isEnabled: Boolean): Boolean {
        return repository.setPhantomPower(channelId, isEnabled)
    }
}