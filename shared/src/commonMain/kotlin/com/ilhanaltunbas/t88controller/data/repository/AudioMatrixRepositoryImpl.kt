package com.ilhanaltunbas.t88controller.data.repository

import com.ilhanaltunbas.t88controller.data.remote.MatrixMessage
import com.ilhanaltunbas.t88controller.data.remote.ProtocolParser
import com.ilhanaltunbas.t88controller.data.remote.TcpSocketClient
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class AudioMatrixRepositoryImpl(
    private val tcpClient: TcpSocketClient
) : AudioMatrixRepository {

    private val commandMutex = Mutex()
    private val parser = ProtocolParser()
    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    private val _matrixUpdates = MutableSharedFlow<MatrixMessage>(extraBufferCapacity = 64)
    override val matrixUpdates: SharedFlow<MatrixMessage> = _matrixUpdates.asSharedFlow()

    // --- MERKEZİ STATE YÖNETİMİ ---
    private val _inputChannels = MutableStateFlow(List(8) { id -> ChannelState(id = id + 1, name = "IN ${id + 1}", isInput = true) })
    override val inputChannels: StateFlow<List<ChannelState>> = _inputChannels.asStateFlow()

    private val _outputChannels = MutableStateFlow(List(8) { id -> ChannelState(id = id + 1, name = "OUT ${id + 1}", isInput = false) })
    override val outputChannels: StateFlow<List<ChannelState>> = _outputChannels.asStateFlow()

    private val _activeRoutes = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    override val activeRoutes: StateFlow<Set<Pair<Int, Int>>> = _activeRoutes.asStateFlow()

    private val _currentPreset = MutableStateFlow(1)
    override val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    private val _isMasterMuted = MutableStateFlow(false)
    override val isMasterMuted: StateFlow<Boolean> = _isMasterMuted.asStateFlow()

    private val pendingResponses = mutableMapOf<String, CompletableDeferred<Any>>()

    init {
        repositoryScope.launch {
            tcpClient.receivedPackets.collect { packet ->
                val message = parser.parse(packet)
                processIncomingMessage(message)
                _matrixUpdates.emit(message)
                resolvePendingResponse(message)
            }
        }
    }

    private fun processIncomingMessage(message: MatrixMessage) {
        when (message) {
            is MatrixMessage.VolumeUpdate -> updateChannel(message.channelId, message.channelType == 1) { it.copy(volume = message.volume.toFloat()) }
            is MatrixMessage.MuteUpdate -> updateChannel(message.channelId, message.channelType == 1) { it.copy(isMuted = message.isMuted) }
            is MatrixMessage.PhantomUpdate -> updateChannel(message.channelId, true) { it.copy(isPhantomOn = message.isEnabled) }
            is MatrixMessage.LineMicUpdate -> updateChannel(message.channelId, true) { it.copy(isLineMode = message.isLine) }
            is MatrixMessage.AfcUpdate -> updateChannel(message.channelId, true) { it.copy(afcLevel = message.level) }
            is MatrixMessage.RoutingUpdate -> {
                val route = Pair(message.inputId, message.outputId)
                _activeRoutes.update { if (message.isRouted) it + route else it - route }
            }
            is MatrixMessage.PresetUpdate -> _currentPreset.value = message.presetId
            is MatrixMessage.MasterMuteUpdate -> _isMasterMuted.value = message.isMuted
            else -> {}
        }
    }

    private fun updateChannel(id: Int, isInput: Boolean, update: (ChannelState) -> ChannelState) {
        if (isInput) {
            _inputChannels.update { list -> list.map { if (it.id == id) update(it) else it } }
        } else {
            _outputChannels.update { list -> list.map { if (it.id == id) update(it) else it } }
        }
    }

    private fun resetState() {
        _inputChannels.value = List(8) { id -> ChannelState(id = id + 1, name = "IN ${id + 1}", isInput = true) }
        _outputChannels.value = List(8) { id -> ChannelState(id = id + 1, name = "OUT ${id + 1}", isInput = false) }
        _activeRoutes.value = emptySet()
        _currentPreset.value = 1
        _isMasterMuted.value = false
    }

    private fun resolvePendingResponse(message: MatrixMessage) {
        val key = when (message) {
            is MatrixMessage.VolumeUpdate -> "04_${message.channelType}_${message.channelId}"
            is MatrixMessage.MuteUpdate -> "03_${message.channelType}_${message.channelId}"
            is MatrixMessage.MasterMuteUpdate -> "03_master"
            is MatrixMessage.RoutingUpdate -> "09_${message.inputId}_${message.outputId}"
            is MatrixMessage.PhantomUpdate -> "07_${message.channelId}"
            is MatrixMessage.LineMicUpdate -> "06_${message.channelId}"
            is MatrixMessage.AfcUpdate -> "08_${message.channelId}"
            is MatrixMessage.PresetUpdate -> "02"
            is MatrixMessage.CameraUpdate -> "0A"
            else -> null
        }
        key?.let { pendingResponses.remove(it)?.complete(message) }
    }

    private suspend fun <T> awaitResponse(key: String, timeout: Long = 2000): T? {
        val deferred = CompletableDeferred<Any>()
        pendingResponses[key] = deferred
        return withTimeoutOrNull(timeout) {
            deferred.await() as? T
        }
    }

    override suspend fun connectToDevice(ip: String, port: Int): Boolean {
        resetState()
        return tcpClient.connect(ip, port)
    }
    override suspend fun disconnectDevice() = tcpClient.disconnect()
    override fun getConnectionStatus(): StateFlow<ConnectionStatus> = tcpClient.connectionStatus

    private suspend fun sendCommandSafely(commandPacket: ByteArray): Boolean {
        return commandMutex.withLock {
            val isSent = tcpClient.sendBytes(commandPacket)
            delay(200)
            isSent
        }
    }

    override suspend fun setMuteState(channelType: Int, channel: Int, isMuted: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x03.toByte(), 0x03.toByte(), channelType.toByte(), channel.toByte(), (if (isMuted) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setMasterOutputMute(isMuted: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x03.toByte(), 0x02.toByte(), 0x03.toByte(), (if (isMuted) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setVolume(channelType: Int, channel: Int, volumeLevel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x04.toByte(), 0x04.toByte(), channelType.toByte(), channel.toByte(), volumeLevel.toByte(), 0x00.toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setRelativeVolume(channelType: Int, channel: Int, isIncrease: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x05.toByte(), 0x03.toByte(), channelType.toByte(), channel.toByte(), (if (isIncrease) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun recallPreset(presetId: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x02.toByte(), 0x01.toByte(), presetId.toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setRouting(inputChannel: Int, outputChannel: Int, isRouted: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x09.toByte(), 0x03.toByte(), inputChannel.toByte(), outputChannel.toByte(), (if (isRouted) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setCameraPosition(channelId: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x0A.toByte(), 0x01.toByte(), channelId.toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setLineMicMode(channel: Int, isLine: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x06.toByte(), 0x02.toByte(), channel.toByte(), (if (isLine) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setPhantomPower(channel: Int, isEnabled: Boolean): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x07.toByte(), 0x02.toByte(), channel.toByte(), (if (isEnabled) 0x01 else 0x00).toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun setFeedbackSuppression(channel: Int, level: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x08.toByte(), 0x02.toByte(), channel.toByte(), level.toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun getMuteState(channelType: Int, channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x03.toByte(), 0x02.toByte(), channelType.toByte(), channel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.MuteUpdate>("03_${channelType}_${channel}")
        return response?.isMuted ?: false
    }

    override suspend fun getVolume(channelType: Int, channel: Int): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x04.toByte(), 0x02.toByte(), channelType.toByte(), channel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.VolumeUpdate>("04_${channelType}_${channel}")
        return response?.volume ?: 50
    }

    override suspend fun getLineMicMode(channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x06.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.LineMicUpdate>("06_${channel}")
        return response?.isLine ?: true
    }

    override suspend fun getPhantomPowerState(channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x07.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.PhantomUpdate>("07_${channel}")
        return response?.isEnabled ?: false
    }

    override suspend fun getFeedbackSuppression(channel: Int): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x08.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.AfcUpdate>("08_${channel}")
        return response?.level ?: 0
    }

    override suspend fun getRouting(inputChannel: Int, outputChannel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x09.toByte(), 0x02.toByte(), inputChannel.toByte(), outputChannel.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.RoutingUpdate>("09_${inputChannel}_${outputChannel}")
        return response?.isRouted ?: false
    }

    override suspend fun getCurrentPreset(): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x02.toByte(), 0x00.toByte(), 0xEE.toByte())
        sendCommandSafely(commandPacket)
        val response = awaitResponse<MatrixMessage.PresetUpdate>("02")
        return response?.presetId ?: 1
    }
}