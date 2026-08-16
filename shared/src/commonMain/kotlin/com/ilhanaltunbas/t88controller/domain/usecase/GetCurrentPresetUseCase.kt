package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetCurrentPresetUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(): Int {
        return repository.getCurrentPreset()
    }
}