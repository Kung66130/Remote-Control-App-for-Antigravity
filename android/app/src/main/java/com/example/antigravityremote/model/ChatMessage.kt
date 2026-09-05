package com.example.antigravityremote.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    var content: String,
    var thought: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
