package com.example.antigravityremote.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.antigravityremote.model.ChatMessage
import com.example.antigravityremote.model.ChatSession
import com.example.antigravityremote.model.ConnectionState
import com.example.antigravityremote.model.DiscoveredDevice
import com.example.antigravityremote.model.ToolApprovalRequest
import com.example.antigravityremote.model.UserProfile
import com.example.antigravityremote.network.AgentStreamEvent
import com.example.antigravityremote.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "RemoteViewModel"
    private val prefs: SharedPreferences = application.getSharedPreferences("antigravity_pairing_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_SAVED_REMOTE_URL = "saved_remote_url"
        private const val PREF_DEVICE_ID = "paired_device_id"
        private const val PREF_EMAIL = "paired_email"
        private const val PREF_TOKEN = "paired_token"
        private const val PREF_NAME = "paired_name"
    }

    private val wsManager = WebSocketManager(viewModelScope)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val connectionState: StateFlow<ConnectionState> = wsManager.connectionState

    var relayServerUrl = MutableStateFlow("https://relay.kunglab.online")
    var authToken = MutableStateFlow("")

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activeStreamingMessage = MutableStateFlow<ChatMessage?>(null)
    val activeStreamingMessage: StateFlow<ChatMessage?> = _activeStreamingMessage.asStateFlow()

    private val _pendingToolApproval = MutableStateFlow<ToolApprovalRequest?>(null)
    val pendingToolApproval: StateFlow<ToolApprovalRequest?> = _pendingToolApproval.asStateFlow()

    private val _agentStatus = MutableStateFlow("idle")
    val agentStatus: StateFlow<String> = _agentStatus.asStateFlow()

    private val sessionHistory = mutableMapOf<String, List<ChatMessage>>()
    private val _currentSessionId = MutableStateFlow("session-default")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _projectTree = MutableStateFlow<List<com.example.antigravityremote.model.ProjectGroup>>(emptyList())
    val projectTree: StateFlow<List<com.example.antigravityremote.model.ProjectGroup>> = _projectTree.asStateFlow()

    private val _runningTasks = MutableStateFlow<List<com.example.antigravityremote.model.RunningTask>>(emptyList())
    val runningTasks: StateFlow<List<com.example.antigravityremote.model.RunningTask>> = _runningTasks.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(
        listOf(ChatSession(id = "session-default", title = "Current Chat", lastMessage = "Active pair-programming session"))
    )
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    var antigravityUrlInput = MutableStateFlow("https://antigravity.google.com/r/384e5c79-d94a-4948-be65-0d39ae06e2a5-v2")
    private val _activeWebUrl = MutableStateFlow<String?>(null)
    val activeWebUrl: StateFlow<String?> = _activeWebUrl.asStateFlow()

    var hostInput = MutableStateFlow("127.0.0.1")
    var portInput = MutableStateFlow("8080")
    var tokenInput = MutableStateFlow("antigravity-secret-key")
    var chatInput = MutableStateFlow("")

    fun openAntigravityUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isNotBlank()) {
            antigravityUrlInput.value = cleanUrl
            prefs.edit().putString(PREF_SAVED_REMOTE_URL, cleanUrl).apply()
            _activeWebUrl.value = cleanUrl
        }
    }

    fun closeAntigravityUrl() {
        _activeWebUrl.value = null
    }

    fun unpairAndScanNew() {
        prefs.edit().remove(PREF_SAVED_REMOTE_URL).apply()
        _activeWebUrl.value = null
    }

    fun handleScannedQrCode(qrData: String) {
        val raw = qrData.trim()
        if (raw.isNotBlank()) {
            openAntigravityUrl(raw)
        }
    }

    init {
        viewModelScope.launch {
            wsManager.events.collect { event ->
                handleEvent(event)
            }
        }

        // Automatic Reconnect to Paired Antigravity Remote on launch! (One-Time Pairing)
        val savedUrl = prefs.getString(PREF_SAVED_REMOTE_URL, null)
        if (!savedUrl.isNullOrBlank()) {
            _activeWebUrl.value = savedUrl
            antigravityUrlInput.value = savedUrl
        }
    }

    fun signInWithGoogle(email: String, displayName: String = "") {
        viewModelScope.launch {
            _isScanning.value = true
            withContext(Dispatchers.IO) {
                try {
                    val authEndpoint = "${relayServerUrl.value.trim().removeSuffix("/")}/api/auth/google"
                    val jsonBody = JSONObject().apply {
                        put("email", email)
                        if (displayName.isNotBlank()) put("name", displayName)
                    }
                    val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                    val req = Request.Builder().url(authEndpoint).post(body).build()
                    val res = httpClient.newCall(req).execute()
                    
                    if (res.isSuccessful) {
                        val respBody = res.body?.string() ?: "{}"
                        val json = JSONObject(respBody)
                        authToken.value = json.optString("token")
                        val userEmail = json.optString("email", email)
                        val userName = json.optString("name", displayName.ifBlank { email.substringBefore("@") })
                        
                        _currentUser.value = UserProfile(
                            email = userEmail,
                            displayName = userName
                        )
                        fetchDevicesFromRelay()
                    } else {
                        // Local fallback for offline/local simulation
                        _currentUser.value = UserProfile(email = email, displayName = displayName.ifBlank { email.substringBefore("@") })
                        fetchDevicesFromRelay()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Auth error: ${e.message}", e)
                    _currentUser.value = UserProfile(email = email, displayName = displayName.ifBlank { email.substringBefore("@") })
                    fetchDevicesFromRelay()
                }
            }
            _isScanning.value = false
        }
    }

    fun signOut() {
        disconnect()
        _currentUser.value = null
        _discoveredDevices.value = emptyList()
        authToken.value = ""
    }

    fun scanDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            withContext(Dispatchers.IO) {
                fetchDevicesFromRelay()
            }
            _isScanning.value = false
        }
    }

    private fun fetchDevicesFromRelay() {
        val found = mutableListOf<DiscoveredDevice>()
        try {
            val endpoint = "${relayServerUrl.value.trim().removeSuffix("/")}/api/devices"
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${authToken.value}")
                .build()
            val res = httpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: "{}"
                val json = JSONObject(body)
                val arr = json.optJSONArray("devices")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val d = arr.getJSONObject(i)
                        found.add(
                            DiscoveredDevice(
                                deviceId = d.optString("deviceId"),
                                deviceName = d.optString("deviceName"),
                                email = if (d.has("email")) d.optString("email") else null,
                                localIp = if (d.has("localIp")) d.optString("localIp") else "Cloud Relay",
                                tailscaleIp = if (d.has("tailscaleIp")) d.optString("tailscaleIp") else null,
                                port = d.optInt("port", 8000),
                                workspace = if (d.has("workspace")) d.optString("workspace") else "",
                                status = d.optString("status", "online")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch devices error: ${e.message}", e)
        }

        if (found.isEmpty()) {
            found.add(
                DiscoveredDevice(
                    deviceId = "my-host-pc",
                    deviceName = "My Computer (Antigravity Bridge)",
                    email = _currentUser.value?.email,
                    localIp = "Cloud Relay (4G/5G Ready)",
                    tailscaleIp = null,
                    port = 8000,
                    workspace = "Active Pair Programming Session",
                    status = "online"
                )
            )
        }

        _discoveredDevices.value = found
    }

    fun connectToDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            if (authToken.value.isBlank()) {
                val email = _currentUser.value?.email ?: (device.email ?: "kung66130@gmail.com")
                withContext(Dispatchers.IO) {
                    try {
                        val authEndpoint = "${relayServerUrl.value.trim().removeSuffix("/")}/api/auth/google"
                        val jsonBody = JSONObject().apply { put("email", email) }
                        val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                        val req = Request.Builder().url(authEndpoint).post(body).build()
                        val res = httpClient.newCall(req).execute()
                        if (res.isSuccessful) {
                            val respBody = res.body?.string() ?: "{}"
                            val json = JSONObject(respBody)
                            authToken.value = json.optString("token")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get auth token before connect: ${e.message}", e)
                    }
                }
            }

            // Save for automatic reconnect (One-Time Pairing)
            prefs.edit()
                .putString(PREF_DEVICE_ID, device.deviceId)
                .putString(PREF_EMAIL, device.email ?: _currentUser.value?.email ?: "kung66130@gmail.com")
                .putString(PREF_TOKEN, authToken.value)
                .putString(PREF_NAME, device.deviceName)
                .apply()

            wsManager.connectRelay(relayServerUrl.value, authToken.value, device.deviceId)
        }
    }

    fun unpairAndDisconnect() {
        prefs.edit().clear().apply()
        disconnect()
        _currentUser.value = null
    }

    private fun handleEvent(event: AgentStreamEvent) {
        when (event) {
            is AgentStreamEvent.ThoughtChunk -> {
                val current = _activeStreamingMessage.value
                if (current != null && current.id == event.messageId) {
                    val updatedThought = (current.thought ?: "") + event.chunk
                    _activeStreamingMessage.value = current.copy(thought = updatedThought)
                } else {
                    val newMsg = ChatMessage(
                        id = event.messageId,
                        role = "assistant",
                        content = "",
                        thought = event.chunk
                    )
                    _activeStreamingMessage.value = newMsg
                }
            }
            is AgentStreamEvent.TokenChunk -> {
                val current = _activeStreamingMessage.value
                if (current != null && current.id == event.messageId) {
                    val updatedContent = current.content + event.chunk
                    _activeStreamingMessage.value = current.copy(content = updatedContent)
                } else {
                    val newMsg = ChatMessage(
                        id = event.messageId,
                        role = "assistant",
                        content = event.chunk,
                        thought = ""
                    )
                    _activeStreamingMessage.value = newMsg
                }
            }
            is AgentStreamEvent.MessageComplete -> {
                val completeMsg = event.message
                _activeStreamingMessage.value = null
                _messages.value = _messages.value + completeMsg
            }
            is AgentStreamEvent.ToolRequest -> {
                _pendingToolApproval.value = event.request
            }
            is AgentStreamEvent.SyncSessions -> {
                if (event.projectTree.isNotEmpty()) {
                    _projectTree.value = event.projectTree
                }
                if (event.sessions.isNotEmpty()) {
                    _sessions.value = event.sessions
                }
                if (event.activeSessionId.isNotEmpty()) {
                    if (_currentSessionId.value == "session-default") {
                        _currentSessionId.value = event.activeSessionId
                    }
                }
                if (event.messages.isNotEmpty() && (_currentSessionId.value == "session-default" || _currentSessionId.value == event.activeSessionId)) {
                    _messages.value = event.messages
                    sessionHistory[event.activeSessionId] = event.messages
                }
            }
            is AgentStreamEvent.SyncMessages -> {
                _messages.value = event.messages
                sessionHistory[event.sessionId] = event.messages
                if (_currentSessionId.value == "session-default") {
                    _currentSessionId.value = event.sessionId
                }
            }
            is AgentStreamEvent.IncomingPcMessage -> {
                if (_currentSessionId.value == "session-default" || event.sessionId == _currentSessionId.value) {
                    _currentSessionId.value = event.sessionId
                    val currentList = _messages.value.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.id == event.message.id }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = event.message
                    } else {
                        val isDuplicate = currentList.isNotEmpty() &&
                            currentList.last().role == event.message.role &&
                            currentList.last().content == event.message.content
                        if (!isDuplicate) {
                            currentList.add(event.message)
                        }
                    }
                    _messages.value = currentList
                    sessionHistory[event.sessionId] = currentList

                    val text = event.message.content
                    val snippet = if (text.length > 30) text.take(30) + "..." else text
                    _sessions.value = _sessions.value.map { s ->
                        if (s.id == event.sessionId) {
                            s.copy(lastMessage = snippet, timestamp = System.currentTimeMillis())
                        } else s
                    }
                } else {
                    val bgList = (sessionHistory[event.sessionId] ?: emptyList()).toMutableList()
                    val existingIndex = bgList.indexOfFirst { it.id == event.message.id }
                    if (existingIndex >= 0) {
                        bgList[existingIndex] = event.message
                    } else {
                        bgList.add(event.message)
                    }
                    sessionHistory[event.sessionId] = bgList
                }
            }
            is AgentStreamEvent.TaskStatusUpdate -> {
                _runningTasks.value = event.runningTasks
            }
            is AgentStreamEvent.StatusUpdate -> {
                _agentStatus.value = event.status
            }
            is AgentStreamEvent.ErrorEvent -> {
                _messages.value = _messages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "system",
                    content = "⚠️ Error: ${event.message}"
                )
            }
        }
    }

    fun connect() {
        val port = portInput.value.toIntOrNull() ?: 8080
        wsManager.connectDirect(hostInput.value, port, tokenInput.value)
    }

    fun disconnect() {
        wsManager.disconnect()
    }

    fun sendMessage() {
        val text = chatInput.value.trim()
        if (text.isEmpty()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text
        )
        _messages.value = _messages.value + userMsg
        chatInput.value = ""

        // Update current session title & snippet
        val snippet = if (text.length > 30) text.take(30) + "..." else text
        _sessions.value = _sessions.value.map { session ->
            if (session.id == _currentSessionId.value) {
                val updatedTitle = if (session.title == "Current Chat" || session.title == "New Chat") snippet else session.title
                session.copy(title = updatedTitle, lastMessage = text, timestamp = System.currentTimeMillis())
            } else {
                session
            }
        }
        sessionHistory[_currentSessionId.value] = _messages.value

        wsManager.sendChat(text)
    }

    fun createNewSession() {
        sessionHistory[_currentSessionId.value] = _messages.value
        val newId = "session-${UUID.randomUUID().toString().take(8)}"
        val newSession = ChatSession(
            id = newId,
            title = "New Chat",
            lastMessage = "Ready to start",
            timestamp = System.currentTimeMillis()
        )
        _sessions.value = listOf(newSession) + _sessions.value
        _currentSessionId.value = newId
        _messages.value = emptyList()
        _activeStreamingMessage.value = null
    }

    fun switchSession(sessionId: String) {
        if (sessionId == _currentSessionId.value) return
        sessionHistory[_currentSessionId.value] = _messages.value
        _currentSessionId.value = sessionId
        val cached = sessionHistory[sessionId]
        if (cached != null) {
            _messages.value = cached
        } else {
            _messages.value = emptyList()
        }
        _activeStreamingMessage.value = null
        wsManager.sendLoadSession(sessionId)
    }

    fun deleteSession(sessionId: String) {
        sessionHistory.remove(sessionId)
        val updated = _sessions.value.filterNot { it.id == sessionId }
        _sessions.value = if (updated.isEmpty()) {
            listOf(ChatSession(id = "session-default", title = "New Chat"))
        } else {
            updated
        }
        if (_currentSessionId.value == sessionId) {
            val nextId = _sessions.value.first().id
            _currentSessionId.value = nextId
            _messages.value = sessionHistory[nextId] ?: emptyList()
        }
    }

    fun sendQuickCommand(cmd: String) {
        chatInput.value = cmd
        sendMessage()
    }

    fun approveTool(requestId: String) {
        wsManager.sendToolApproval(requestId, "approve")
        _pendingToolApproval.value = null
    }

    fun rejectTool(requestId: String, reason: String = "Rejected by user from mobile") {
        wsManager.sendToolApproval(requestId, "reject", reason)
        _pendingToolApproval.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
        _activeStreamingMessage.value = null
        sessionHistory[_currentSessionId.value] = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
