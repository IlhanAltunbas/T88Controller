package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetRelativeVolumeUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelType: Int, channelId: Int, isIncrease: Boolean): Boolean {
        return repository.setRelativeVolume(channelType, channelId, isIncrease)
    }
}