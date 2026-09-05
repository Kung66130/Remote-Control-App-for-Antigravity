package com.example.antigravityremote.network

import android.util.Log
import com.example.antigravityremote.model.ChatMessage
import com.example.antigravityremote.model.ChatSession
import com.example.antigravityremote.model.ConnectionState
import com.example.antigravityremote.model.ToolApprovalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AgentStreamEvent {
    data class ThoughtChunk(val messageId: String, val chunk: String) : AgentStreamEvent()
    data class TokenChunk(val messageId: String, val chunk: String) : AgentStreamEvent()
    data class MessageComplete(val message: ChatMessage) : AgentStreamEvent()
    data class ToolRequest(val request: ToolApprovalRequest) : AgentStreamEvent()
    data class StatusUpdate(val status: String, val message: String? = null) : AgentStreamEvent()
    data class SyncSessions(
        val activeSessionId: String,
        val projectTree: List<com.example.antigravityremote.model.ProjectGroup>,
        val sessions: List<ChatSession>,
        val messages: List<ChatMessage>
    ) : AgentStreamEvent()
    data class SyncMessages(
        val sessionId: String,
        val messages: List<ChatMessage>
    ) : AgentStreamEvent()
    data class IncomingPcMessage(
        val sessionId: String,
        val message: ChatMessage
    ) : AgentStreamEvent()
    data class TaskStatusUpdate(
        val runningTasks: List<com.example.antigravityremote.model.RunningTask>
    ) : AgentStreamEvent()
    data class ErrorEvent(val message: String) : AgentStreamEvent()
}

class WebSocketManager(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    private val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<AgentStreamEvent>()
    val events: SharedFlow<AgentStreamEvent> = _events.asSharedFlow()

    private var currentHost: String = ""
    private var currentPort: Int = 8000
    private var currentToken: String = ""

    fun connectRelay(relayBaseUrl: String, token: String, deviceId: String) {
        val trimmedUrl = relayBaseUrl.trim().removeSuffix("/")
        val wsScheme = if (trimmedUrl.startsWith("https")) "wss" else "ws"
        val netloc = trimmedUrl.replace("https://", "").replace("http://", "")
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val encodedDeviceId = java.net.URLEncoder.encode(deviceId, "UTF-8")
        val wsUrl = "$wsScheme://$netloc/ws/client?token=$encodedToken&device_id=$encodedDeviceId"

        currentHost = netloc
        currentToken = token
        _connectionState.value = ConnectionState.Connecting

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Central Relay: $wsUrl")
                _connectionState.value = ConnectionState.Connected(
                    host = currentHost,
                    port = currentPort,
                    workspace = "Antigravity Cloud Session",
                    version = "2.0.0"
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Relay Connection Failure: ${t.message}", t)
                val errMsg = t.localizedMessage ?: "Relay connection failed"
                _connectionState.value = ConnectionState.Error(errMsg)
            }
        })
    }

    fun connectDirect(host: String, port: Int, token: String) {
        currentHost = host.trim()
        currentPort = port
        currentToken = token.trim()

        _connectionState.value = ConnectionState.Connecting

        val wsUrl = if (currentToken.isNotEmpty()) {
            "ws://$currentHost:$currentPort/ws/agent?token=$currentToken"
        } else {
            "ws://$currentHost:$currentPort/ws/agent"
        }

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Direct WebSocket Opened: $wsUrl")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Direct WebSocket Failure: ${t.message}", t)
                val errMsg = t.localizedMessage ?: "Connection failed"
                _connectionState.value = ConnectionState.Error(errMsg)
            }
        })
    }

    private suspend fun handleIncomingMessage(rawJson: String) {
        try {
            val obj = JSONObject(rawJson)
            val type = obj.optString("type")

            when (type) {
                "connected" -> {
                    val payload = obj.optJSONObject("payload")
                    val workspace = payload?.optString("workspace") ?: "Antigravity Workspace"
                    val version = payload?.optString("version") ?: "2.0.0"
                    _connectionState.value = ConnectionState.Connected(
                        host = currentHost,
                        port = currentPort,
                        workspace = workspace,
                        version = version
                    )
                }
                "sync_sessions" -> {
                    val activeId = obj.optString("activeSessionId", "")
                    val projectTreeArr = obj.optJSONArray("projectTree")
                    val sessionsArr = obj.optJSONArray("sessions")
                    val messagesArr = obj.optJSONArray("messages")

                    val projectTreeList = mutableListOf<com.example.antigravityremote.model.ProjectGroup>()
                    if (projectTreeArr != null) {
                        for (i in 0 until projectTreeArr.length()) {
                            val p = projectTreeArr.getJSONObject(i)
                            val pSessionsArr = p.optJSONArray("sessions")
                            val pSessions = mutableListOf<ChatSession>()
                            if (pSessionsArr != null) {
                                for (j in 0 until pSessionsArr.length()) {
                                    val s = pSessionsArr.getJSONObject(j)
                                    pSessions.add(
                                        ChatSession(
                                            id = s.optString("id"),
                                            title = s.optString("title"),
                                            lastMessage = s.optString("lastMessage"),
                                            timestamp = s.optLong("timestamp", System.currentTimeMillis()),
                                            relativeTime = s.optString("relativeTime", ""),
                                            projectName = s.optString("projectName", p.optString("projectName")),
                                            isActive = s.optBoolean("isActive", false)
                                        )
                                    )
                                }
                            }
                            projectTreeList.add(
                                com.example.antigravityremote.model.ProjectGroup(
                                    projectName = p.optString("projectName"),
                                    latestTimestamp = p.optLong("latestTimestamp", 0L),
                                    sessions = pSessions
                                )
                            )
                        }
                    }

                    val sessionsList = mutableListOf<ChatSession>()
                    if (sessionsArr != null) {
                        for (i in 0 until sessionsArr.length()) {
                            val s = sessionsArr.getJSONObject(i)
                            sessionsList.add(
                                ChatSession(
                                    id = s.optString("id"),
                                    title = s.optString("title"),
                                    lastMessage = s.optString("lastMessage"),
                                    timestamp = s.optLong("timestamp", System.currentTimeMillis()),
                                    relativeTime = s.optString("relativeTime", ""),
                                    projectName = s.optString("projectName", "General"),
                                    isActive = s.optBoolean("isActive", false)
                                )
                            )
                        }
                    }

                    val messagesList = parseMessages(messagesArr)
                    _events.emit(AgentStreamEvent.SyncSessions(activeId, projectTreeList, sessionsList, messagesList))
                }
                "sync_messages" -> {
                    val sid = obj.optString("sessionId", "")
                    val messagesArr = obj.optJSONArray("messages")
                    val messagesList = parseMessages(messagesArr)
                    _events.emit(AgentStreamEvent.SyncMessages(sid, messagesList))
                }
                "incoming_pc_message" -> {
                    val sid = obj.optString("sessionId", "")
                    val mObj = obj.optJSONObject("message")
                    if (mObj != null) {
                        val chatMsg = ChatMessage(
                            id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                            role = mObj.optString("role", "user"),
                            content = mObj.optString("content", "")
                        )
                        _events.emit(AgentStreamEvent.IncomingPcMessage(sid, chatMsg))
                    }
                }
                "task_status" -> {
                    val tasksArr = obj.optJSONArray("runningTasks")
                    val taskList = mutableListOf<com.example.antigravityremote.model.RunningTask>()
                    if (tasksArr != null) {
                        for (i in 0 until tasksArr.length()) {
                            val t = tasksArr.getJSONObject(i)
                            taskList.add(
                                com.example.antigravityremote.model.RunningTask(
                                    id = t.optString("id"),
                                    command = t.optString("command"),
                                    toolName = t.optString("toolName", "run_command"),
                                    toolAction = t.optString("toolAction"),
                                    status = t.optString("status", "running")
                                )
                            )
                        }
                    }
                    _events.emit(AgentStreamEvent.TaskStatusUpdate(taskList))
                }
                "thought_chunk" -> {
                    val msgId = obj.optString("messageId")
                    val content = obj.optString("content")
                    _events.emit(AgentStreamEvent.ThoughtChunk(msgId, content))
                }
                "token_chunk" -> {
                    val msgId = obj.optString("messageId")
                    val content = obj.optString("content")
                    _events.emit(AgentStreamEvent.TokenChunk(msgId, content))
                }
                "message_complete" -> {
                    val msgObj = obj.optJSONObject("message")
                    if (msgObj != null) {
                        val chatMsg = ChatMessage(
                            id = msgObj.optString("id"),
                            role = msgObj.optString("role"),
                            content = msgObj.optString("content"),
                            thought = if (msgObj.has("thought")) msgObj.optString("thought") else ""
                        )
                        _events.emit(AgentStreamEvent.MessageComplete(chatMsg))
                    }
                }
                "tool_request" -> {
                    val payload = obj.optJSONObject("payload")
                    if (payload != null) {
                        val req = ToolApprovalRequest(
                            requestId = payload.optString("requestId"),
                            toolName = payload.optString("toolName"),
                            toolSummary = payload.optString("toolSummary"),
                            toolAction = payload.optString("toolAction"),
                            commandLine = if (payload.has("commandLine")) payload.optString("commandLine") else null,
                            targetFile = if (payload.has("targetFile")) payload.optString("targetFile") else null,
                            timestamp = payload.optDouble("timestamp", 0.0)
                        )
                        _events.emit(AgentStreamEvent.ToolRequest(req))
                    }
                }
                "status_update" -> {
                    val status = obj.optString("status")
                    val message = if (obj.has("message")) obj.optString("message") else null
                    _events.emit(AgentStreamEvent.StatusUpdate(status, message))
                }
                "error" -> {
                    val errMsg = obj.optString("message", "Unknown error")
                    _events.emit(AgentStreamEvent.ErrorEvent(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing packet: $rawJson", e)
        }
    }

    private fun parseMessages(arr: JSONArray?): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        if (arr == null) return list
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            list.add(
                ChatMessage(
                    id = m.optString("id", i.toString()),
                    role = m.optString("role", "user"),
                    content = m.optString("content", ""),
                    thought = if (m.has("thought")) m.optString("thought") else ""
                )
            )
        }
        return list
    }

    fun sendLoadSession(sessionId: String) {
        val json = JSONObject().apply {
            put("type", "load_session")
            put("sessionId", sessionId)
        }
        webSocket?.send(json.toString())
    }

    fun sendChat(content: String) {
        val json = JSONObject().apply {
            put("type", "chat")
            put("content", content)
        }
        webSocket?.send(json.toString())
    }

    fun sendToolApproval(requestId: String, decision: String, feedback: String? = null) {
        val json = JSONObject().apply {
            put("type", "approve_tool")
            put("requestId", requestId)
            put("decision", decision)
            if (feedback != null) put("feedback", feedback)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }
}
