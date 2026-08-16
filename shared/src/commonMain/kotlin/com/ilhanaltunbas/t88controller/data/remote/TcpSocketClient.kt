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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TcpSocketClient {

    private val selectorManager = SelectorManager(Dispatchers.Default)
    private var socket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private var readChannel: ByteReadChannel? = null
    private var readingJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.Default + Job())

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val receivedPackets = _receivedPackets.asSharedFlow()

    suspend fun connect(ip: String, port: Int = 5000): Boolean {
        // Bağlantı varsa önce kapat
        disconnect()

        // --- TEST (MOCK) BYPASS MODU ---
        if (ip == "1.1.1.1" && port == 1111) {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            delay(1000)
            _connectionStatus.value = ConnectionStatus.CONNECTED
            println("TEST MODU AKTİF: Simülasyon başlatıldı.")
            return true
        }

        // --- GERÇEK SOKET BAĞLANTISI ---
        return withContext(Dispatchers.Default) {
            try {
                _connectionStatus.value = ConnectionStatus.CONNECTING

                socket = aSocket(selectorManager).tcp().connect(ip, port)
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                readChannel = socket?.openReadChannel()

                _connectionStatus.value = ConnectionStatus.CONNECTED
                println("TCP Soket Bağlantısı Başarılı: $ip:$port")

                // Okuma döngüsünü başlat
                startReadingLoop()
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionStatus.value = ConnectionStatus.ERROR
                println("TCP Soket Bağlantı Hatası: ${e.message}")
                false
            }
        }
    }

    private fun startReadingLoop() {
        readingJob = clientScope.launch {
            val channel = readChannel ?: return@launch
            try {
                while (isActive && !channel.isClosedForRead) {
                    // Paket Header'ını bekle: A5 C3 3C 5A
                    if (channel.readByte() == 0xA5.toByte()) {
                        if (channel.readByte() == 0xC3.toByte()) {
                            if (channel.readByte() == 0x3C.toByte()) {
                                if (channel.readByte() == 0x5A.toByte()) {
                                    // Header bulundu, paketin geri kalanını oku
                                    readFullPacket(channel)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Okuma döngüsü hatası: ${e.message}")
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
            }
        }
    }

    private suspend fun readFullPacket(channel: ByteReadChannel) {
        val deviceId = channel.readByte()    // FF
        val commandType = channel.readByte() // 36 or 63
        val functionId = channel.readByte()  // e.g. 03 (Mute)
        val dataLength = channel.readByte().toInt() and 0xFF
        
        val data = ByteArray(dataLength)
        for (i in 0 until dataLength) {
            data[i] = channel.readByte()
        }
        
        val tail = channel.readByte() // EE
        
        if (tail == 0xEE.toByte()) {
            // Tam paketi birleştir ve yayınla
            val fullPacket = byteArrayOf(
                0xA5.toByte(), 0xC3.toByte(), 0x3C.toByte(), 0x5A.toByte(),
                deviceId, commandType, functionId, dataLength.toByte(),
                *data, tail
            )
            _receivedPackets.emit(fullPacket)
        }
    }

    suspend fun sendBytes(bytes: ByteArray): Boolean {
        // --- TEST MODU KORUMASI ---
        if (_connectionStatus.value == ConnectionStatus.CONNECTED && socket == null) {
            println("TEST MODU (Soketsiz Gönderim): ${bytes.toHexString()}")
            return true
        }

        // --- GERÇEK AĞ GÖNDERİMİ ---
        return withContext(Dispatchers.Default) {
            try {
                val channel = writeChannel
                if (channel != null && !channel.isClosedForWrite) {
                    channel.writeFully(bytes)
                    println("TCP Paket Gönderildi: ${bytes.toHexString()}")
                    true
                } else {
                    println("Soket kapalı, paket gönderilemedi!")
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionStatus.value = ConnectionStatus.ERROR
                false
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.Default) {
            try {
                readingJob?.cancelAndJoin()
                readingJob = null
                socket?.close()
                socket = null
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { it.toUByte().toString(16).padStart(2, '0').uppercase() }
    }
}