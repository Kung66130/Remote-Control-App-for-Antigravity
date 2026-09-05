package com.example.antigravityremote.model

import kotlinx.serialization.Serializable

@Serializable
data class ToolApprovalRequest(
    val requestId: String,
    val toolName: String,
    val toolSummary: String,
    val toolAction: String,
    val commandLine: String? = null,
    val targetFile: String? = null,
    val timestamp: Double = 0.0
)
