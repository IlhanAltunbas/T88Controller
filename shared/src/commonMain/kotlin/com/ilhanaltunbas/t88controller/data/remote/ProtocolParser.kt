package com.ilhanaltunbas.t88controller.data.remote

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
    data object Unknown : MatrixMessage()
}

class ProtocolParser {

    fun parse(packet: ByteArray): MatrixMessage {
        if (packet.size < 10) return MatrixMessage.Unknown

        val functionId = packet[6].toInt() and 0xFF
        val data = packet.sliceArray(8 until packet.size - 1)

        return when (functionId) {
            0x04 -> { // Volume
                if (data.size >= 3) {
                    MatrixMessage.VolumeUpdate(data[0].toInt(), data[1].toInt(), data[2].toInt())
                } else MatrixMessage.Unknown
            }
            0x03 -> { // Mute
                if (data.size >= 3) {
                    // Type 0x03 (Output total mute)
                    if (data[0] == 0x03.toByte()) {
                        MatrixMessage.MasterMuteUpdate(data[1] == 0x01.toByte())
                    } else {
                        MatrixMessage.MuteUpdate(data[0].toInt(), data[1].toInt(), data[2] == 0x01.toByte())
                    }
                } else MatrixMessage.Unknown
            }
            0x09 -> { // Routing
                if (data.size >= 3) {
                    MatrixMessage.RoutingUpdate(data[0].toInt(), data[1].toInt(), data[2] == 0x01.toByte())
                } else MatrixMessage.Unknown
            }
            0x07 -> { // Phantom
                if (data.size >= 2) {
                    MatrixMessage.PhantomUpdate(data[0].toInt(), data[1] == 0x01.toByte())
                } else MatrixMessage.Unknown
            }
            0x06 -> { // Line/Mic
                if (data.size >= 2) {
                    MatrixMessage.LineMicUpdate(data[0].toInt(), data[1] == 0x01.toByte())
                } else MatrixMessage.Unknown
            }
            0x08 -> { // AFC
                if (data.size >= 2) {
                    MatrixMessage.AfcUpdate(data[0].toInt(), data[1].toInt())
                } else MatrixMessage.Unknown
            }
            0x02 -> { // Preset
                if (data.size >= 1) {
                    MatrixMessage.PresetUpdate(data[0].toInt())
                } else MatrixMessage.Unknown
            }
            0x0A -> { // Camera
                if (data.size >= 1) {
                    MatrixMessage.CameraUpdate(data[0].toInt())
                } else MatrixMessage.Unknown
            }
            else -> MatrixMessage.Unknown
        }
    }
}