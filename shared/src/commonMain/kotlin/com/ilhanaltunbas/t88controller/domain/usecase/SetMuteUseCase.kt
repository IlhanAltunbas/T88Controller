package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetMuteUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelType: Int, channelId: Int, isMuted: Boolean): Boolean {
        return repository.setMuteState(channelType, channelId, isMuted)
    }
}