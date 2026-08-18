package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository

class UpdateChannelNameUseCase(private val repository: AudioMatrixRepository) {
    operator fun invoke(id: Int, isInput: Boolean, newName: String) {
        repository.updateChannelName(id, isInput, newName)
    }
}