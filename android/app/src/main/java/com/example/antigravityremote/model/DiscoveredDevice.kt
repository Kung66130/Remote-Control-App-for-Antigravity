package com.example.antigravityremote.model

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val email: String? = null,
    val localIp: String,
    val tailscaleIp: String? = null,
    val port: Int = 8080,
    val workspace: String = "",
    val status: String = "online"
)

@Serializable
data class UserProfile(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)
