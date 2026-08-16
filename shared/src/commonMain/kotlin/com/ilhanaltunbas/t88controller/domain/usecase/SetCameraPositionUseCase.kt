package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetCameraPositionUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelId: Int): Boolean {
        return repository.setCameraPosition(channelId)
    }
}