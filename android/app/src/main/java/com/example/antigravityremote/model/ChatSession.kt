package com.example.antigravityremote.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val relativeTime: String = "",
    val projectName: String = "General",
    val isActive: Boolean = false
)

@Serializable
data class ProjectGroup(
    val projectName: String,
    val latestTimestamp: Long = 0L,
    val sessions: List<ChatSession> = emptyList()
)
