package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.usecase.MixerUseCases
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MixerViewModel(
    private val useCases: MixerUseCases
) : ViewModel() {

    val inputChannels: StateFlow<List<ChannelState>> = useCases.observeInputChannels()
    val outputChannels: StateFlow<List<ChannelState>> = useCases.observeOutputChannels()
    val connectionStatus = useCases.observeConnectionStatus()

    init {
        observeConnectionStatus()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionStatus.collectLatest { status ->
                if (status == ConnectionStatus.CONNECTED) {
                    fetchInitialData()
                }
            }
        }
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeout(10000.milliseconds) {
                    (1..8).forEach { id ->
                        launch { useCases.getVolume(1, id) }
                        launch { useCases.getMuteState(1, id) }
                        launch { useCases.getLineMicMode(id) }
                        launch { useCases.getPhantomPowerState(id) }
                        launch { useCases.getFeedbackSuppression(id) }
                    }
                    (1..8).forEach { id ->
                        launch { useCases.getVolume(2, id) }
                        launch { useCases.getMuteState(2, id) }
                    }
                }
            } catch (e: Exception) {
                println("VERİ ÇEKİLİRKEN HATA OLUŞTU: ${e.message}")
            }
        }
    }

    fun changeVolume(channelId: Int, isInput: Boolean, newVolume: Float) {
        viewModelScope.launch {
            useCases.setAbsoluteVolume(if (isInput) 1 else 2, channelId, newVolume.toInt())
        }
    }

    fun stepVolume(channelId: Int, isInput: Boolean, isIncrease: Boolean) {
        viewModelScope.launch {
            useCases.setRelativeVolume(if (isInput) 1 else 2, channelId, isIncrease)
        }
    }

    fun toggleMute(channelId: Int, isInput: Boolean) {
        viewModelScope.launch {
            val currentState = if (isInput) {
                inputChannels.value.find { it.id == channelId }?.isMuted ?: false
            } else {
                outputChannels.value.find { it.id == channelId }?.isMuted ?: false
            }
            useCases.setMute(if (isInput) 1 else 2, channelId, !currentState)
        }
    }

    fun togglePhantomPower(channelId: Int) {
        viewModelScope.launch {
            val currentState = inputChannels.value.find { it.id == channelId }?.isPhantomOn ?: false
            useCases.setPhantomPower(channelId, !currentState)
        }
    }

    fun toggleLineMicMode(channelId: Int) {
        viewModelScope.launch {
            val currentState = inputChannels.value.find { it.id == channelId }?.isLineMode ?: true
            useCases.setLineMicMode(channelId, !currentState)
        }
    }

    fun changeAfcLevel(channelId: Int, level: Int) {
        viewModelScope.launch {
            useCases.setFeedbackSuppression(channelId, level)
        }
    }
}