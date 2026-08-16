package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class GetRoutingUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(inputChannel: Int, outputChannel: Int): Boolean {
        return repository.getRouting(inputChannel, outputChannel)
    }
}