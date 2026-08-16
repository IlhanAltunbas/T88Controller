package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetAbsoluteVolumeUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelType: Int, channelId: Int, volumeLevel: Int): Boolean {
        return repository.setVolume(channelType, channelId, volumeLevel)
    }
}