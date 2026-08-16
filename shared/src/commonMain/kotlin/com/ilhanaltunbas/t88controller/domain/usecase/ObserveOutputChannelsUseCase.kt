package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveOutputChannelsUseCase(private val repository: AudioMatrixRepository) {
    operator fun invoke(): StateFlow<List<ChannelState>> = repository.outputChannels
}