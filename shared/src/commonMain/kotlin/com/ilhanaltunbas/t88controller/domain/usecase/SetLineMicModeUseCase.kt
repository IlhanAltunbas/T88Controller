package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetLineMicModeUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelId: Int, isLine: Boolean): Boolean {
        return repository.setLineMicMode(channelId, isLine)
    }
}