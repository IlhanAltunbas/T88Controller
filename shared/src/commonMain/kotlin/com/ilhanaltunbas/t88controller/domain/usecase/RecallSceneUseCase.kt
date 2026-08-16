package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class RecallSceneUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(sceneId: Int): Boolean {
        return repository.recallPreset(sceneId)
    }
}