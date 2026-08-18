package com.ilhanaltunbas.t88controller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChannelState(
    val id: Int,
    val name: String,
    val isInput: Boolean,
    val volume: Float = 0f, // Varsayılan 0.0 dB
    val gain: Float = 0f,   // Varsayılan 0.0 dB
    val isMuted: Boolean = false,
    val isPhantomOn: Boolean = false,
    val isLineMode: Boolean = true,
    val afcLevel: Int = 0
)