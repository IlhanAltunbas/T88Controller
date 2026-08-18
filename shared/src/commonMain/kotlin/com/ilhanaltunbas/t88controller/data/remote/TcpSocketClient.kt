package com.ilhanaltunbas.t88controller.data.remote

import com.ilhanaltunbas.t88controller.domain.model.ConnectionStatus
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class TcpSocketClient {

    private val selectorManager = SelectorManager(Dispatchers.Default)
    private var socket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private var readChannel: ByteReadChannel? = null
    private var readingJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.Default + Job())

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    val receivedPackets = _receivedPackets.asSharedFlow()

    private val mockRoutes = mutableSetOf<Pair<Int, Int>>()
    private val mockVolumes = mutableMapOf<String, Int>()
    private val mockStates = mutableMapOf<String, Int>()

    suspend fun connect(ip: String, port: Int = 5000): Boolean {
        disconnect()
        if (ip == "1.1.1.1" && port == 1111) {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            delay(300)
            _connectionStatus.value = ConnectionStatus.CONNECTED
            startMockReadingLoop()
            return true
        }
        return withContext(Dispatchers.Default) {
            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                withTimeout(5000) {
                    socket = aSocket(selectorManager).tcp().connect(ip, port)
                }
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                readChannel = socket?.openReadChannel()
                _connectionStatus.value = ConnectionStatus.CONNECTED
                startReadingLoop()
                true
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.ERROR
                false
            }
        }
    }

    private fun startReadingLoop() {
        readingJob = clientScope.launch {
            val channel = readChannel ?: return@launch
            try {
                while (isActive && !channel.isClosedForRead) {
                    if (channel.readByte() == 0xA5.toByte()) {
                        if (channel.readByte() == 0xC3.toByte()) {
                            if (channel.readByte() == 0x3C.toByte()) {
                                if (channel.readByte() == 0x5A.toByte()) {
                                    readFullPacket(channel)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
            }
        }
    }

    private suspend fun readFullPacket(channel: ByteReadChannel) {
        val deviceId = channel.readByte()
        val commandType = channel.readByte()
        val functionId = channel.readByte()
        val dataLength = channel.readByte().toInt() and 0xFF
        val data = ByteArray(dataLength) { channel.readByte() }
        val tail = channel.readByte()
        if (tail == 0xEE.toByte()) {
            val fullPacket = byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), deviceId, commandType, functionId, dataLength.toByte(), *data, tail)
            _receivedPackets.emit(fullPacket)
        }
    }

    private fun startMockReadingLoop() {
        readingJob = clientScope.launch {
            delay(200)
            _receivedPackets.emit(byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x03.toByte(), 0x02.toByte(), 0x03.toByte(), 0x00.toByte(), 0xEE.toByte()))
        }
    }

    suspend fun sendBytes(bytes: ByteArray): Boolean {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED && socket == null) {
            clientScope.launch {
                delay(15)
                processMockRequest(bytes)
            }
            return true
        }
        return withContext(Dispatchers.Default) {
            try {
                writeChannel?.let { if (!it.isClosedForWrite) { it.writeFully(bytes); true } else false } ?: false
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.ERROR
                false
            }
        }
    }

    private suspend fun processMockRequest(request: ByteArray) {
        if (request.size < 9) return
        val isRead = request[5] == 0x63.toByte()
        val functionId = request[6]
        val dataLen = request[7].toInt()

        val response: ByteArray? = when (functionId) {
            0x09.toByte() -> { // Routing
                val inCh = request[8].toInt(); val outCh = request[9].toInt()
                if (!isRead) {
                    if (request[10] == 0x01.toByte()) mockRoutes.add(Pair(inCh, outCh)) else mockRoutes.remove(Pair(inCh, outCh))
                }
                val isActive = mockRoutes.contains(Pair(inCh, outCh))
                byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x09.toByte(), 0x03.toByte(), inCh.toByte(), outCh.toByte(), (if (isActive) 0x01 else 0x00).toByte(), 0xEE.toByte())
            }
            0x04.toByte() -> { // Volume
                val type = request[8]; val ch = request[9]
                val normalizedType = if (type == 0x00.toByte()) 0x02.toByte() else type
                val key = "vol_${normalizedType}_$ch"
                if (!isRead) mockVolumes[key] = request[10].toInt()
                val vol = mockVolumes[key] ?: 0
                byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x04.toByte(), 0x03.toByte(), normalizedType, ch, vol.toByte(), 0xEE.toByte())
            }
            0x03.toByte() -> { // Mute / Master Mute
                val isMaster = (dataLen == 2 && request[8] == 0x03.toByte()) || (dataLen == 3 && request[8] == 0x03.toByte())
                if (isMaster) {
                    if (!isRead) mockStates["master_mute"] = request[9].toInt()
                    val s = mockStates["master_mute"] ?: 0
                    byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x03.toByte(), 0x02.toByte(), 0x03.toByte(), s.toByte(), 0xEE.toByte())
                } else {
                    val type = request[8]; val ch = request[9]
                    val normalizedType = if (type == 0x00.toByte()) 0x02.toByte() else type
                    val key = "mute_${normalizedType}_$ch"
                    if (!isRead) mockStates[key] = request[10].toInt()
                    val s = mockStates[key] ?: 0
                    byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x03.toByte(), 0x03.toByte(), normalizedType, ch, s.toByte(), 0xEE.toByte())
                }
            }
            0x05.toByte() -> { // Relative Volume
                val type = request[8]; val ch = request[9]; val dir = request[10]
                val normalizedType = if (type == 0x00.toByte()) 0x02.toByte() else type
                val key = "vol_${normalizedType}_$ch"
                val current = mockVolumes[key] ?: 0
                val next = if (dir == 0x00.toByte()) (current + 1).coerceAtMost(12) else (current - 1).coerceAtLeast(-60)
                mockVolumes[key] = next
                byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x04.toByte(), 0x03.toByte(), normalizedType, ch, next.toByte(), 0xEE.toByte())
            }
            0x02.toByte() -> { // Archive (Preset)
                if (!isRead && request.size >= 10) mockStates["preset"] = request[8].toInt()
                val p = mockStates["preset"] ?: 1
                byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), 0x02.toByte(), 0x01.toByte(), p.toByte(), 0xEE.toByte())
            }
            0x06.toByte(), 0x07.toByte(), 0x08.toByte() -> { // Details
                val ch = request[8]
                val key = "det_${functionId}_$ch"
                if (!isRead) mockStates[key] = request[9].toInt()
                val s = mockStates[key] ?: (if (functionId == 0x06.toByte()) 1 else 0)
                byteArrayOf(0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(), 0xFF.toByte(), 0x36.toByte(), functionId, 0x02.toByte(), ch, s.toByte(), 0xEE.toByte())
            }
            else -> if (!isRead) request else null
        }
        response?.let { _receivedPackets.emit(it) }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.Default) {
            try {
                readingJob?.cancelAndJoin(); readingJob = null
                socket?.close(); socket = null
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: Exception) { }
        }
    }
}