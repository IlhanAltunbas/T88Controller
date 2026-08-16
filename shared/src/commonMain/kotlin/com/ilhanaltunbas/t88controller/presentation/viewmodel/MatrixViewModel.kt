package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.usecase.GetRoutingUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveActiveRoutesUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.ObserveConnectionStatusUseCase
import com.ilhanaltunbas.t88controller.domain.usecase.SetRoutingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatrixViewModel(
    private val setRoutingUseCase: SetRoutingUseCase,
    private val getRoutingUseCase: GetRoutingUseCase,
    private val observeActiveRoutesUseCase: ObserveActiveRoutesUseCase,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase
) : ViewModel() {

    val activeRoutes: StateFlow<Set<Pair<Int, Int>>> = observeActiveRoutesUseCase()
    val connectionStatus = observeConnectionStatusUseCase()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var batchJob: Job? = null

    init {
        observeConnectionStatus()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionStatus.collectLatest { status ->
                if (status == ConnectionStatus.CONNECTED) {
                    fetchInitialRoutes()
                }
            }
        }
    }

    private fun fetchInitialRoutes() {
        viewModelScope.launch {
            for (input in 1..8) {
                for (output in 1..8) {
                    launch { getRoutingUseCase(input, output) }
                }
            }
        }
    }

    fun toggleRoute(inputId: Int, outputId: Int) {
        // Manuel bir dokunuş gelirse, eğer varsa toplu temizleme işlemini durdur
        batchJob?.cancel()
        _isProcessing.value = false

        val isCurrentlyRouted = activeRoutes.value.contains(Pair(inputId, outputId))
        viewModelScope.launch {
            setRoutingUseCase(inputId, outputId, !isCurrentlyRouted)
        }
    }

    fun clearAllRoutes() {
        val currentRoutes = activeRoutes.value
        if (currentRoutes.isEmpty()) return

        // Eski bir işlem varsa iptal et ve yenisini başlat
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _isProcessing.value = true
            try {
                // Rotaları sıralı (Sequential) olarak temizle. 
                // Repository'deki Mutex ve 200ms delay burada güvenliği sağlayacak.
                currentRoutes.forEach { route ->
                    setRoutingUseCase(route.first, route.second, false)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }
}