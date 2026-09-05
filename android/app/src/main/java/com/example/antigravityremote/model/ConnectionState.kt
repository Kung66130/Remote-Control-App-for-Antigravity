package com.example.antigravityremote.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(
        val host: String,
        val port: Int,
        val workspace: String,
        val version: String
    ) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
