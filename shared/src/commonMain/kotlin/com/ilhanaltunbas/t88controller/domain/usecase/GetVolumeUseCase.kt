package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetVolumeUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelType: Int, channel: Int): Int {
        return repository.getVolume(channelType, channel)
    }
}