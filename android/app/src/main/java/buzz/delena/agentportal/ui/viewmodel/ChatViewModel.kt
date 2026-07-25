package buzz.delena.agentportal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.MessageEntity
import buzz.delena.agentportal.core.network.ConnectionState
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

/**
 * Skeleton realtime wiring: connects the STOMP client and subscribes to this
 * session's topic, but treats every inbound frame as a generic "something
 * changed, refetch" signal rather than parsing the backend's full event
 * schema -- that schema isn't modeled on the Android side yet (deliberately
 * out of scope for this skeleton, see docs/ROADMAP.md). Messages and pending
 * permissions are refreshed by REST poll on session open, after sending a
 * prompt, and on every STOMP frame for this session.
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
                    stompClient.subscribeToSession(sessionId).collect {
                        refresh()
                    }
                }
            }
        }
    }

    fun onPromptChange(value: String) {
        _state.value = _state.value.copy(promptDraft = value)
    }

    fun sendPrompt() {
        val prompt = _state.value.promptDraft
        if (prompt.isBlank() || _state.value.isSending) return

        _state.value = _state.value.copy(isSending = true, promptDraft = "")
        viewModelScope.launch {
            sessionRepository.sendPrompt(sessionId, prompt)
            _state.value = _state.value.copy(isSending = false)
            refreshPermissions()
        }
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
            _state.value = _state.value.copy(pendingPermission = null)
            PermissionApprovalNotifier.cancelPermissionNotification(appContext, permissionId)
            refresh()
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
