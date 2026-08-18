package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveActiveRoutesUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveConnectionStatusUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.SetRoutingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MatrixViewModel(
    private val setRoutingUseCase: SetRoutingUseCase,
    private val observeActiveRoutesUseCase: ObserveActiveRoutesUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val settingsRepository: com.ilhanaltunbas.t88controller.domain.repository.SettingsRepository
) : ViewModel() {

    val activeRoutes: StateFlow<Set<Pair<Int, Int>>> = observeActiveRoutesUseCase()
    val connectionStatus = observeConnectionStatusUseCase()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _selectedRoute = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedRoute: StateFlow<Pair<Int, Int>?> = _selectedRoute.asStateFlow()

    private val _matrixGains = MutableStateFlow<Map<Pair<Int, Int>, Float>>(emptyMap())
    val matrixGains: StateFlow<Map<Pair<Int, Int>, Float>> = _matrixGains.asStateFlow()

    private var batchJob: Job? = null
    
    // IP bazlı hafıza anahtarı prefixi
    private val ipPrefix get() = "matrix_${settingsRepository.getLastIp()}_"

    init {
        loadMatrixGains()
    }

    private fun loadMatrixGains() {
        val gains = mutableMapOf<Pair<Int, Int>, Float>()
        for (i in 1..8) {
            for (o in 1..8) {
                val saved = settingsRepository.getChannelName(ipPrefix, i * 10 + o, true, "")
                if (saved.isNotEmpty()) {
                    saved.toFloatOrNull()?.let { gains[Pair(i, o)] = it }
                }
            }
        }
        _matrixGains.value = gains
    }

    fun toggleRoute(inputId: Int, outputId: Int) {
        val targetRoute = Pair(inputId, outputId)
        val isCurrentlyRouted = activeRoutes.value.contains(targetRoute)
        val isCurrentlySelected = _selectedRoute.value == targetRoute

        when {
            !isCurrentlyRouted -> {
                _selectedRoute.value = targetRoute
                viewModelScope.launch { setRoutingUseCase(inputId, outputId, true) }
            }
            isCurrentlyRouted && !isCurrentlySelected -> {
                _selectedRoute.value = targetRoute
            }
            isCurrentlyRouted && isCurrentlySelected -> {
                _selectedRoute.value = null
                viewModelScope.launch { setRoutingUseCase(inputId, outputId, false) }
            }
        }
        batchJob?.cancel()
        _isProcessing.value = false
    }

    fun updateMatrixGain(inputId: Int, outputId: Int, gain: Float) {
        val clampedGain = gain.coerceIn(-60f, 12f)
        val route = Pair(inputId, outputId)
        _matrixGains.update { it + (route to clampedGain) }
        
        settingsRepository.saveChannelName(ipPrefix, inputId * 10 + outputId, true, clampedGain.toString())
    }

    fun resetToDefaultMatrix() {
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _isProcessing.value = true
            _selectedRoute.value = null
            
            _matrixGains.value = emptyMap()
            for (i in 1..8) {
                for (o in 1..8) {
                    settingsRepository.saveChannelName(ipPrefix, i * 10 + o, true, "")
                }
            }

            try {
                for (inCh in 1..8) {
                    for (outCh in 1..8) {
                        val shouldBeActive = (inCh == outCh)
                        val isCurrentlyActive = activeRoutes.value.contains(Pair(inCh, outCh))
                        if (isCurrentlyActive != shouldBeActive) {
                            setRoutingUseCase(inCh, outCh, shouldBeActive)
                        }
                    }
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearAllRoutes() {
        val currentRoutes = activeRoutes.value
        _selectedRoute.value = null
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _isProcessing.value = true
            
            _matrixGains.value = emptyMap()
            for (i in 1..8) {
                for (o in 1..8) {
                    settingsRepository.saveChannelName(ipPrefix, i * 10 + o, true, "")
                }
            }

            try {
                currentRoutes.forEach { route ->
                    setRoutingUseCase(route.first, route.second, false)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetGainOfSelected() {
        _selectedRoute.value?.let { (input, output) ->
            updateMatrixGain(input, output, 0.0f)
        }
    }

    fun disconnectSelectedRoute() {
        _selectedRoute.value?.let { (input, output) ->
            viewModelScope.launch {
                setRoutingUseCase(input, output, false)
                _selectedRoute.value = null
            }
        }
    }
}