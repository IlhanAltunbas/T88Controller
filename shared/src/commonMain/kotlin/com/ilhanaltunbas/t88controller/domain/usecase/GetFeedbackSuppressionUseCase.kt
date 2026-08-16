package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetFeedbackSuppressionUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(channel: Int): Int {
        return repository.getFeedbackSuppression(channel)
    }
}