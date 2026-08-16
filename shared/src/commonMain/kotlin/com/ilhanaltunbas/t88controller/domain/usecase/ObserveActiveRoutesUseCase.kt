package com.ilhanaltunbas.t88controller.domain.usecase

import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveActiveRoutesUseCase(private val repository: AudioMatrixRepository) {
    operator fun invoke(): StateFlow<Set<Pair<Int, Int>>> = repository.activeRoutes
}