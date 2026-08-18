package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveCameraPositionUseCase(private val repository: AudioMatrixRepository) {
    operator fun invoke(): StateFlow<Int> = repository.cameraPosition
}