package com.ilhanaltunbas.t88controller.domain.repository

import com.ilhanaltunbas.t88controller.data.remote.MatrixMessage
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AudioMatrixRepository {

    val matrixUpdates: SharedFlow<MatrixMessage>

    // --- MERKEZİ STATE (STATEFLOW) ---
    val inputChannels: StateFlow<List<ChannelState>>
    val outputChannels: StateFlow<List<ChannelState>>
    val activeRoutes: StateFlow<Set<Pair<Int, Int>>>
    val currentPreset: StateFlow<Int>
    val isMasterMuted: StateFlow<Boolean>
    val isSyncing: StateFlow<Boolean>
    val cameraPosition: StateFlow<Int>

    suspend fun connectToDevice(ip: String, port: Int): Boolean
    suspend fun disconnectDevice()
    fun getConnectionStatus(): StateFlow<ConnectionStatus>
    suspend fun syncAllData()

    // --- KONTROL (WRITE) METOTLARI ---
    suspend fun setMuteState(channelType: Int, channel: Int, isMuted: Boolean): Boolean
    suspend fun setVolume(channelType: Int, channel: Int, volumeLevel: Int): Boolean
    suspend fun recallPreset(presetId: Int): Boolean
    suspend fun setRouting(inputChannel: Int, outputChannel: Int, isRouted: Boolean): Boolean

    suspend fun setRelativeVolume(channelType: Int, channel: Int, isIncrease: Boolean): Boolean
    suspend fun setMasterOutputMute(isMuted: Boolean): Boolean
    suspend fun setCameraPosition(channelId: Int): Boolean

    suspend fun setLineMicMode(channel: Int, isLine: Boolean): Boolean
    suspend fun setPhantomPower(channel: Int, isEnabled: Boolean): Boolean
    suspend fun setFeedbackSuppression(channel: Int, level: Int): Boolean
    fun updateChannelName(id: Int, isInput: Boolean, newName: String)

    // --- SORGULAMA (READ) METOTLARI ---
    suspend fun getMuteState(channelType: Int, channel: Int): Boolean
    suspend fun getVolume(channelType: Int, channel: Int): Int

    suspend fun getLineMicMode(channel: Int): Boolean
    suspend fun getPhantomPowerState(channel: Int): Boolean
    suspend fun getFeedbackSuppression(channel: Int): Int

    suspend fun getCurrentPreset(): Int
}