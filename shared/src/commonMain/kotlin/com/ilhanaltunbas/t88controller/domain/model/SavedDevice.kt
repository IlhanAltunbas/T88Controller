package com.ilhanaltunbas.t88controller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedDevice(
    val name: String,
    val ip: String,
    val port: Int
)