package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetMuteStateUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelType: Int, channel: Int): Boolean {
        return repository.getMuteState(channelType, channel)
    }
}