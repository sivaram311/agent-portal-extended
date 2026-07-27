package buzz.delena.agentportal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.MessageEntity
import buzz.delena.agentportal.core.network.ConnectionState
import buzz.delena.agentportal.core.network.NetworkModule
import buzz.delena.agentportal.core.network.StompWebSocketClient
import buzz.delena.agentportal.core.network.dto.PermissionStatus
import buzz.delena.agentportal.notifications.PermissionApprovalNotifier
import buzz.delena.agentportal.ui.screens.ChatMessageItem
import buzz.delena.agentportal.ui.screens.ChatUiState
import buzz.delena.agentportal.ui.screens.PendingPermissionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

// Mirrors the backend's AgentEventDto (com.agentportal.dto), the exact shape
// SessionEventBus sends as each STOMP MESSAGE frame body. payload is
// intentionally a loose JsonObject (its shape varies per event type) rather
// than a fixed data class.
@Serializable
private data class StompAgentEvent(
    @SerialName("sessionId") val sessionId: String? = null,
    val type: String,
    val payload: JsonObject? = null,
)

/**
 * Realtime wiring: connects the STOMP client and subscribes to this
 * session's topic. assistant_delta / thinking_delta frames (the backend's
 * real per-token streaming events, emitted from AgentBridge as the Cursor
 * ACP session/update stream arrives) are appended directly into the
 * in-progress assistant message for a live typewriter effect -- they are
 * NOT persisted to Room themselves, so the local streaming message uses a
 * client-generated placeholder id. Every other event type falls back to the
 * original "something changed, refetch over REST" behavior, which also
 * naturally supersedes the streaming placeholder with the real
 * backend-persisted message once refreshMessages() completes (Room emits a
 * fresh list keyed by the real id, replacing the whole in-memory list).
 */
class ChatViewModel(
    private val sessionId: String,
    initialTitle: String,
    private val sessionRepository: SessionRepository,
    private val stompClient: StompWebSocketClient,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(sessionTitle = initialTitle))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamingMessageId: String? = null
    private val streamingBuffer = StringBuilder()

    init {
        viewModelScope.launch {
            sessionRepository.observeMessages(sessionId).collect { entities ->
                _state.value = _state.value.copy(messages = entities.map { it.toItem() })
            }
        }
        refresh()

        stompClient.connect()
        viewModelScope.launch {
            stompClient.connectionState.collect { connectionState ->
                if (connectionState == ConnectionState.CONNECTED) {
                    stompClient.subscribeToSession(sessionId).collect { event ->
                        handleStompEvent(event.bodyJson)
                    }
                }
            }
        }
    }

    private fun handleStompEvent(bodyJson: String) {
        val event = runCatching {
            NetworkModule.json.decodeFromString(StompAgentEvent.serializer(), bodyJson)
        }.getOrNull()

        // Unparseable frame (e.g. a STOMP control frame, not an AgentEventDto) --
        // fall back to the safe original behavior rather than dropping it silently.
        if (event == null) {
            refresh()
            return
        }

        when (event.type) {
            "assistant_delta" -> appendStreamingDelta(event.payload)
            // thinking_delta is Cursor's internal reasoning trace, not part of
            // the final assistant message -- no bubble model for it yet
            // (ChatMessageItem has no "thinking" variant), so it's a no-op
            // rather than corrupting the visible transcript with it.
            "thinking_delta" -> Unit
            else -> {
                // Any non-delta event (assistant_message, permission_required,
                // run_completed, etc.) means the streaming turn this buffer was
                // tracking is over one way or another -- clear it so a stale
                // placeholder can't linger, then reconcile from REST as before.
                streamingMessageId = null
                streamingBuffer.setLength(0)
                refresh()
            }
        }
    }

    private fun appendStreamingDelta(payload: JsonObject?) {
        val text = payload?.get("text")?.jsonPrimitive?.content
        if (text.isNullOrEmpty()) return

        streamingBuffer.append(text)
        val currentId = streamingMessageId

        if (currentId == null) {
            val newId = "streaming-${System.currentTimeMillis()}"
            streamingMessageId = newId
            _state.value = _state.value.copy(
                messages = _state.value.messages + ChatMessageItem(
                    id = newId,
                    isUser = false,
                    contentMarkdown = streamingBuffer.toString(),
                    timeLabel = "now",
                ),
            )
        } else {
            _state.value = _state.value.copy(
                messages = _state.value.messages.map { message ->
                    if (message.id == currentId) {
                        message.copy(contentMarkdown = streamingBuffer.toString())
                    } else {
                        message
                    }
                },
            )
        }
    }

    fun onPromptChange(value: String) {
        _state.value = _state.value.copy(promptDraft = value)
    }

    fun sendPrompt() {
        val prompt = _state.value.promptDraft
        if (prompt.isBlank() || _state.value.isSending) return

        // Restore the draft on failure -- previously this was cleared
        // unconditionally, so a failed send silently discarded what the
        // user typed with no way to recover it.
        _state.value = _state.value.copy(isSending = true, promptDraft = "", error = null)
        viewModelScope.launch {
            sessionRepository.sendPrompt(sessionId, prompt)
                .onSuccess {
                    _state.value = _state.value.copy(isSending = false)
                    refreshPermissions()
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        promptDraft = prompt,
                        error = errorMessage(t),
                    )
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun approvePermission(permissionId: String) {
        decidePermission(permissionId, PermissionStatus.ALLOW_ONCE.name)
    }

    fun rejectPermission(permissionId: String) {
        decidePermission(permissionId, PermissionStatus.REJECT_ONCE.name)
    }

    private fun decidePermission(permissionId: String, decision: String) {
        viewModelScope.launch {
            sessionRepository.decidePermission(sessionId, permissionId, decision, reason = null)
                .onSuccess {
                    _state.value = _state.value.copy(pendingPermission = null, error = null)
                    PermissionApprovalNotifier.cancelPermissionNotification(appContext, permissionId)
                    refresh()
                }
                .onFailure { t ->
                    // Deliberately keep pendingPermission as-is on failure --
                    // the card stays visible so the user can retry, rather
                    // than silently vanishing as if it had been resolved.
                    _state.value = _state.value.copy(error = errorMessage(t))
                }
        }
    }

    private fun errorMessage(t: Throwable): String {
        val httpCode = (t as? retrofit2.HttpException)?.code()
        return when {
            httpCode == 401 -> "Your session expired and couldn't be refreshed — please sign in again."
            httpCode != null -> "Server error ($httpCode). Please try again."
            else -> t.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach the server. Check your connection and try again."
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            runCatching { sessionRepository.refreshMessages(sessionId) }
            refreshPermissions()
        }
    }

    private suspend fun refreshPermissions() {
        sessionRepository.getPendingPermissions(sessionId).onSuccess { permissions ->
            val pending = permissions.firstOrNull { it.status == PermissionStatus.PENDING }
            val previousId = _state.value.pendingPermission?.id
            _state.value = _state.value.copy(
                pendingPermission = pending?.let {
                    PendingPermissionItem(
                        id = it.id,
                        toolLabel = it.kind ?: "Tool permission",
                        detail = it.planMarkdown ?: it.detailsJson,
                    )
                },
            )
            // Only post on a genuinely new pending permission, not every poll
            // tick that still finds the same one -- avoids re-notifying for
            // something the user already sees a notification for.
            if (pending != null && pending.id != previousId) {
                PermissionApprovalNotifier.postPermissionNotification(
                    context = appContext,
                    sessionId = sessionId,
                    permissionId = pending.id,
                    toolLabel = pending.kind ?: "Tool permission",
                    detail = pending.planMarkdown ?: pending.detailsJson,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompClient.disconnect()
    }

    private fun MessageEntity.toItem() = ChatMessageItem(
        id = id,
        isUser = role == "USER",
        contentMarkdown = content,
        timeLabel = createdAt,
    )

    class Factory(
        private val sessionId: String,
        private val initialTitle: String,
        private val sessionRepository: SessionRepository,
        private val stompClient: StompWebSocketClient,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(sessionId, initialTitle, sessionRepository, stompClient, appContext) as T
        }
    }
}
