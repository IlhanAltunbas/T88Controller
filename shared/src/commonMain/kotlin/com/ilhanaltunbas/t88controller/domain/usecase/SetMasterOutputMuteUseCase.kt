package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetMasterOutputMuteUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(isMuted: Boolean): Boolean {
        return repository.setMasterOutputMute(isMuted)
    }
}