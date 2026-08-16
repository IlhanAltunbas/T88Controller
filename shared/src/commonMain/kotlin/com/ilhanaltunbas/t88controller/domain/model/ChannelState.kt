package com.ilhanaltunbas.t88controller.domain.model

data class ChannelState(
    val id: Int, // 1'den 8'e kadar
    val name: String,
    val isInput: Boolean, // True ise Input, False ise Output kanalıdır
    val volume: Float = 75f,
    val gain: Float = 50f, // Sadece Input kanalları için geçerli
    val isMuted: Boolean = false,
    val isPhantomOn: Boolean = false, // Sadece Input
    val isLineMode: Boolean = true, // Sadece Input
    val afcLevel: Int = 0 // Sadece Input
)