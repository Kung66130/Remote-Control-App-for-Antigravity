package com.example.antigravityremote.model

import kotlinx.serialization.Serializable

@Serializable
data class RunningTask(
    val id: String = "",
    val command: String = "",
    val toolName: String = "run_command",
    val toolAction: String = "",
    val status: String = "running",
    val timestamp: Long = System.currentTimeMillis()
)
