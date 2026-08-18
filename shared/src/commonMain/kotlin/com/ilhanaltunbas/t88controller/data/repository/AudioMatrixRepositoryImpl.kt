package com.ilhanaltunbas.t88controller.data.repository

import com.ilhanaltunbas.t88controller.data.remote.MatrixMessage
import com.ilhanaltunbas.t88controller.data.remote.ProtocolParser
import com.ilhanaltunbas.t88controller.data.remote.TcpSocketClient
import com.ilhanaltunbas.t88controller.domain.model.ChannelState
import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import com.ilhanaltunbas.t88controller.domain.repository.AudioMatrixRepository
import com.ilhanaltunbas.t88controller.domain.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AudioMatrixRepositoryImpl(
    private val tcpClient: TcpSocketClient,
    private val settingsRepository: SettingsRepository
) : AudioMatrixRepository {

    private val commandMutex = Mutex()
    private val parser = ProtocolParser()
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentIp: String? = null

    private val _matrixUpdates = MutableSharedFlow<MatrixMessage>(extraBufferCapacity = 128)
    override val matrixUpdates: SharedFlow<MatrixMessage> = _matrixUpdates.asSharedFlow()

    private val _inputChannels = MutableStateFlow<List<ChannelState>>(emptyList())
    override val inputChannels: StateFlow<List<ChannelState>> = _inputChannels.asStateFlow()

    private val _outputChannels = MutableStateFlow<List<ChannelState>>(emptyList())
    override val outputChannels: StateFlow<List<ChannelState>> = _outputChannels.asStateFlow()

    private val _activeRoutes = MutableStateFlow<Set<Pair<Int, Int>>>(List(8) { Pair(it + 1, it + 1) }.toSet())
    override val activeRoutes: StateFlow<Set<Pair<Int, Int>>> = _activeRoutes.asStateFlow()

    private val _currentPreset = MutableStateFlow(1)
    override val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    private val _isMasterMuted = MutableStateFlow(false)
    override val isMasterMuted: StateFlow<Boolean> = _isMasterMuted.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _cameraPosition = MutableStateFlow(1)
    override val cameraPosition: StateFlow<Int> = _cameraPosition.asStateFlow()

    private val pendingResponses = mutableMapOf<String, CompletableDeferred<MatrixMessage>>()
    private val responsesMutex = Mutex()
    private var syncJob: Job? = null

    init {
        resetState()
        repositoryScope.launch {
            tcpClient.receivedPackets.collect { packet ->
                val message = parser.parse(packet)
                processIncomingMessage(message)
                _matrixUpdates.emit(message)
                
                val key = getMessageKey(message)
                key?.let { k ->
                    responsesMutex.withLock { pendingResponses.remove(k)?.complete(message) }
                }
            }
        }
    }

    private fun getMessageKey(message: MatrixMessage): String? {
        return when (message) {
            is MatrixMessage.VolumeUpdate -> "04_${message.channelType}_${message.channelId}"
            is MatrixMessage.MuteUpdate -> "03_${message.channelType}_${message.channelId}"
            is MatrixMessage.MasterMuteUpdate -> "03_master"
            is MatrixMessage.RoutingUpdate -> "09_${message.inputId}_${message.outputId}"
            is MatrixMessage.PhantomUpdate -> "07_${message.channelId}"
            is MatrixMessage.LineMicUpdate -> "06_${message.channelId}"
            is MatrixMessage.AfcUpdate -> "08_${message.channelId}"
            is MatrixMessage.PresetUpdate -> "02"
            is MatrixMessage.CameraUpdate -> "0A"
            is MatrixMessage.RelativeVolumeUpdate -> "05_${message.channelType}_${message.channelId}"
            else -> null
        }
    }

    private fun processIncomingMessage(message: MatrixMessage) {
        when (message) {
            is MatrixMessage.VolumeUpdate -> {
                // TİP NORMALİZASYONU: 0 (Relative Out) veya 2 (Absolute Out) gelirse Output kabul et
                val isInput = message.channelType == 1
                updateChannel(message.channelId, isInput) { it.copy(volume = message.volume.toFloat()) }
            }
            is MatrixMessage.MuteUpdate -> {
                val isInput = message.channelType == 1
                updateChannel(message.channelId, isInput) { it.copy(isMuted = message.isMuted) }
            }
            is MatrixMessage.PhantomUpdate -> updateChannel(message.channelId, true) { it.copy(isPhantomOn = message.isEnabled) }
            is MatrixMessage.LineMicUpdate -> updateChannel(message.channelId, true) { it.copy(isLineMode = message.isLine) }
            is MatrixMessage.AfcUpdate -> updateChannel(message.channelId, true) { it.copy(afcLevel = message.level) }
            is MatrixMessage.RoutingUpdate -> {
                val route = Pair(message.inputId, message.outputId)
                _activeRoutes.update { if (message.isRouted) it + route else it - route }
            }
            is MatrixMessage.PresetUpdate -> _currentPreset.value = message.presetId
            is MatrixMessage.MasterMuteUpdate -> _isMasterMuted.value = message.isMuted
            is MatrixMessage.CameraUpdate -> _cameraPosition.value = message.channelId
            is MatrixMessage.RelativeVolumeUpdate -> {
                val isInput = message.channelType == 1
                updateChannel(message.channelId, isInput) { current ->
                    val newVol = if (message.isIncrease) current.volume + 1f else current.volume - 1f
                    current.copy(volume = newVol.coerceIn(-60f, 12f))
                }
            }
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
        val ip = currentIp ?: ""
        _inputChannels.value = List(8) { id -> 
            val chId = id + 1
            val savedName = settingsRepository.getChannelName(ip, chId, true, "IN $chId")
            ChannelState(id = chId, name = savedName, isInput = true) 
        }
        _outputChannels.value = List(8) { id -> 
            val chId = id + 1
            val savedName = settingsRepository.getChannelName(ip, chId, false, "OUT $chId")
            ChannelState(id = chId, name = savedName, isInput = false) 
        }
        _activeRoutes.value = List(8) { Pair(it + 1, it + 1) }.toSet()
        _currentPreset.value = 1
        _isMasterMuted.value = false // Düzeltildi: val re-assignment hatası
        _cameraPosition.value = 1
    }

    private suspend fun <T> sendAndAwait(key: String, command: ByteArray, timeout: Long = 800): T? {
        val deferred = CompletableDeferred<MatrixMessage>()
        responsesMutex.withLock { pendingResponses[key] = deferred }
        val isSent = sendCommandSafely(command)
        if (!isSent) { responsesMutex.withLock { pendingResponses.remove(key) }; return null }
        return withTimeoutOrNull(timeout) { @Suppress("UNCHECKED_CAST") deferred.await() as? T }
    }

    override suspend fun connectToDevice(ip: String, port: Int): Boolean {
        currentIp = ip
        resetState()
        return tcpClient.connect(ip, port)
    }

    override suspend fun disconnectDevice() {
        syncJob?.cancel(); _isSyncing.value = false; tcpClient.disconnect()
    }

    override fun getConnectionStatus(): StateFlow<ConnectionStatus> = tcpClient.connectionStatus

    override suspend fun syncAllData() {
        syncJob?.cancel()
        syncJob = repositoryScope.launch {
            _isSyncing.value = true
            try {
                getCurrentPreset()
                for (id in 1..8) {
                    getVolume(1, id); getMuteState(1, id)
                    getVolume(2, id); getMuteState(2, id)
                }
                _isSyncing.value = false
                for (id in 1..8) {
                    getLineMicMode(id); getPhantomPowerState(id); getFeedbackSuppression(id)
                }
            } catch (e: Exception) {
                println("SYNC ERROR: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun sendCommandSafely(commandPacket: ByteArray, extraDelay: Long = 0): Boolean {
        return commandMutex.withLock {
            val isSent = tcpClient.sendBytes(commandPacket)
            delay(210 + extraDelay)
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
        val type = if (channelType == 1) 0x01 else 0x00 // Relative Out = 0
        val direction = if (isIncrease) 0x00 else 0x01
        val stepAmount = 0x0A.toByte()
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x05.toByte(), 0x04.toByte(), type.toByte(), channel.toByte(), direction.toByte(), stepAmount, 0xEE.toByte())
        return sendCommandSafely(commandPacket)
    }

    override suspend fun recallPreset(presetId: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x02.toByte(), 0x01.toByte(), presetId.toByte(), 0xEE.toByte())
        return sendCommandSafely(commandPacket, extraDelay = 2890)
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

    override fun updateChannelName(id: Int, isInput: Boolean, newName: String) {
        val ip = currentIp ?: ""
        settingsRepository.saveChannelName(ip, id, isInput, newName)
        updateChannel(id, isInput) { it.copy(name = newName) }
    }

    override suspend fun getMuteState(channelType: Int, channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x03.toByte(), 0x02.toByte(), channelType.toByte(), channel.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.MuteUpdate>("03_${channelType}_${channel}", commandPacket)
        return response?.isMuted ?: false
    }

    override suspend fun getVolume(channelType: Int, channel: Int): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x04.toByte(), 0x02.toByte(), channelType.toByte(), channel.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.VolumeUpdate>("04_${channelType}_${channel}", commandPacket)
        return response?.volume ?: 0
    }

    override suspend fun getLineMicMode(channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x06.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.LineMicUpdate>("06_${channel}", commandPacket)
        return response?.isLine ?: true
    }

    override suspend fun getPhantomPowerState(channel: Int): Boolean {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x07.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.PhantomUpdate>("07_${channel}", commandPacket)
        return response?.isEnabled ?: false
    }

    override suspend fun getFeedbackSuppression(channel: Int): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x08.toByte(), 0x01.toByte(), channel.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.AfcUpdate>("08_${channel}", commandPacket)
        return response?.level ?: 0
    }

    override suspend fun getCurrentPreset(): Int {
        val commandPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x63.toByte(), 0x02.toByte(), 0x01.toByte(), 0xEE.toByte())
        val response = sendAndAwait<MatrixMessage.PresetUpdate>("02", commandPacket)
        return response?.presetId ?: 1
    }
}