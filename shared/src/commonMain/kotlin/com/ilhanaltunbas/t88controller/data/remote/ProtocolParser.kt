package com.ilhanaltunbas.t88controller.data.remote

/**
 * T88 Protokol Mesaj Tipleri.
 */
sealed class MatrixMessage {
    data class VolumeUpdate(val channelType: Int, val channelId: Int, val volume: Int) : MatrixMessage()
    data class MuteUpdate(val channelType: Int, val channelId: Int, val isMuted: Boolean) : MatrixMessage()
    data class RoutingUpdate(val inputId: Int, val outputId: Int, val isRouted: Boolean) : MatrixMessage()
    data class PhantomUpdate(val channelId: Int, val isEnabled: Boolean) : MatrixMessage()
    data class LineMicUpdate(val channelId: Int, val isLine: Boolean) : MatrixMessage()
    data class AfcUpdate(val channelId: Int, val level: Int) : MatrixMessage()
    data class PresetUpdate(val presetId: Int) : MatrixMessage()
    data class MasterMuteUpdate(val isMuted: Boolean) : MatrixMessage()
    data class CameraUpdate(val channelId: Int) : MatrixMessage()
    data class RelativeVolumeUpdate(val channelType: Int, val channelId: Int, val isIncrease: Boolean) : MatrixMessage()
    data object Unknown : MatrixMessage()
}

/**
 * TCP Soketinden gelen ham byte dizilerini 'MatrixMessage' objelerine dönüştürür.
 */
class ProtocolParser {
    fun parse(packet: ByteArray): MatrixMessage {
        // Minimum paket boyutu kontrolü (Header 4 + ID 1 + RW 1 + Func 1 + Len 1 + Tail 1 = 9 byte)
        if (packet.size < 9) return MatrixMessage.Unknown
        
        val functionId = packet[6].toInt()
        val dataLength = packet[7].toInt() and 0xFF
        
        // Veri alanı güvenli aralığı
        if (packet.size < 8 + dataLength) return MatrixMessage.Unknown
        val data = packet.sliceArray(8 until 8 + dataLength)

        return when (functionId) {
            0x04 -> { // Absolute Volume (3 Byte Data)
                if (data.size >= 3) MatrixMessage.VolumeUpdate(data[0].toInt(), data[1].toInt(), data[2].toInt())
                else MatrixMessage.Unknown
            }
            0x03 -> { // Mute / Master Mute
                when {
                    // Kanal Mute: [Type] [ID] [Status] (3 Byte)
                    dataLength == 3 -> MatrixMessage.MuteUpdate(data[0].toInt(), data[1].toInt(), data[2].toInt() == 0x01)
                    // Master Mute: [0x03] [Status] (2 Byte)
                    dataLength == 2 && data[0].toInt() == 0x03 -> MatrixMessage.MasterMuteUpdate(data[1].toInt() == 0x01)
                    else -> MatrixMessage.Unknown
                }
            }
            0x05 -> { // Relative Volume Echo
                if (data.size >= 3) MatrixMessage.RelativeVolumeUpdate(data[0].toInt(), data[1].toInt(), data[2].toInt() == 0x00)
                else MatrixMessage.Unknown
            }
            0x09 -> { // Matrix Routing
                if (data.size >= 3) MatrixMessage.RoutingUpdate(data[0].toInt(), data[1].toInt(), data[2].toInt() == 0x01)
                else MatrixMessage.Unknown
            }
            0x07 -> { // Phantom Power
                if (data.size >= 2) MatrixMessage.PhantomUpdate(data[0].toInt(), data[1].toInt() == 0x01)
                else MatrixMessage.Unknown
            }
            0x06 -> { // Line/Mic Mode
                if (data.size >= 2) MatrixMessage.LineMicUpdate(data[0].toInt(), data[1].toInt() == 0x01)
                else MatrixMessage.Unknown
            }
            0x08 -> { // AFC Level
                if (data.size >= 2) MatrixMessage.AfcUpdate(data[0].toInt(), data[1].toInt())
                else MatrixMessage.Unknown
            }
            0x02 -> { // Preset (Scene)
                // Sahne okuma veya yazma dönüşü
                if (data.size >= 3 && data[0].toInt() == 0x01) MatrixMessage.PresetUpdate(data[2].toInt())
                else if (data.isNotEmpty()) MatrixMessage.PresetUpdate(data[0].toInt())
                else MatrixMessage.Unknown
            }
            0x0A -> { // Camera (One-way usually)
                if (data.isNotEmpty()) MatrixMessage.CameraUpdate(data[0].toInt())
                else MatrixMessage.Unknown
            }
            else -> MatrixMessage.Unknown
        }
    }
}