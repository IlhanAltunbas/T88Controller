package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class SetRoutingUseCase(private val repository: AudioMatrixRepository) {
    suspend operator fun invoke(inputChannel: Int, outputChannel: Int, isRouted: Boolean): Boolean {
        return repository.setRouting(inputChannel, outputChannel, isRouted)
    }
}