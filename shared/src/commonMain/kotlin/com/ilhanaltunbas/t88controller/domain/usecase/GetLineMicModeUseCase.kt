package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetLineMicModeUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channel: Int): Boolean {
        return repository.getLineMicMode(channel)
    }
}