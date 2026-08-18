package com.ilhanaltunbas.t88controller.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.usecase.MixerUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MixerViewModel(
    private val useCases: MixerUseCases
) : ViewModel() {

    // Sayfa geçiş durumunu burada tutuyoruz ki sekme değiştirince sıfırlanmasın
    private val _showInputs = MutableStateFlow(true)
    val showInputs: StateFlow<Boolean> = _showInputs.asStateFlow()

    val inputChannels: StateFlow<List<ChannelState>> = useCases.observeInputChannels()
    
    // Output kanallarını Master Mute ile birleştiriyoruz (Combine)
    val outputChannels: StateFlow<List<ChannelState>> = combine(
        useCases.observeOutputChannels(),
        useCases.observeMasterMute()
    ) { channels, isMasterMuted ->
        // Eğer Master Mute aktifse, tüm çıkış kanallarını 'isMuted = true' olarak göster
        channels.map { it.copy(isMuted = it.isMuted || isMasterMuted) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val connectionStatus = useCases.observeConnectionStatus()

    // Ses ve Afc güncellemelerini yönetmek için iş (Job) haritaları
    private val volumeJobs = mutableMapOf<String, Job>()
    private val afcJobs = mutableMapOf<Int, Job>()

    fun setShowInputs(value: Boolean) {
        _showInputs.value = value
    }

    /**
     * Sürükleme sırasında (Live) veya bırakınca (Final) çağrılan ana ses fonksiyonu.
     * Donanımı yormamak için 'Debounce' ve 'Cancellation' mantığı kullanır.
     */
    fun changeVolume(channelId: Int, isInput: Boolean, newVolume: Float, isFinal: Boolean = false) {
        val type = if (isInput) 1 else 2
        val key = "vol_${type}_$channelId"
        val clampedVol = newVolume.coerceIn(-60f, 12f)

        // Eğer o kanal için hali hazırda bekleyen bir iş varsa iptal et (Sadece en güncel olan gitsin)
        volumeJobs[key]?.cancel()
        
        volumeJobs[key] = viewModelScope.launch {
            if (!isFinal) {
                // Sürükleme sırasında 50ms bekle (Touch jitter engelleme)
                // Repository'deki 200ms lock zaten asıl korumayı sağlıyor.
                delay(50)
            }
            useCases.setAbsoluteVolume(type, channelId, clampedVol.toInt())
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
                // Not: Burada gerçek kanal durumuna bakmalıyız, görselleştirilmiş duruma değil
                useCases.observeOutputChannels().value.find { it.id == channelId }?.isMuted ?: false
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

    fun changeAfcLevel(channelId: Int, level: Int, isFinal: Boolean = false) {
        // Eğer o kanal için bekleyen bir Afc işi varsa iptal et
        afcJobs[channelId]?.cancel()

        afcJobs[channelId] = viewModelScope.launch {
            if (!isFinal) {
                // Sürükleme sırasında donanımı yormamak için kısa bir ara
                delay(50)
            }
            useCases.setFeedbackSuppression(channelId, level)
        }
    }

    fun renameChannel(channelId: Int, isInput: Boolean, newName: String) {
        useCases.updateChannelName(channelId, isInput, newName)
    }
}