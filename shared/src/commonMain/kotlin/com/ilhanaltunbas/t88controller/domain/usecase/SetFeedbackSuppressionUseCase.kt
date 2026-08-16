package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetFeedbackSuppressionUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channelId: Int, level: Int): Boolean {
        return repository.setFeedbackSuppression(channelId, level)
    }
}