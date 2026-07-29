package buzz.delena.agentportal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.AuthRepository
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.MessageEntity
import buzz.delena.agentportal.core.network.ConnectionState
import buzz.delena.agentportal.core.network.NetworkModule
import buzz.delena.agentportal.core.network.StompWebSocketClient
import buzz.delena.agentportal.core.network.dto.FileChangeDto
import buzz.delena.agentportal.core.network.dto.PermissionStatus
import buzz.delena.agentportal.core.network.userFacingErrorMessage
import buzz.delena.agentportal.notifications.PermissionApprovalNotifier
import buzz.delena.agentportal.ui.activity.ToolActivity
import buzz.delena.agentportal.ui.components.ConnectionStatusUi
import buzz.delena.agentportal.ui.components.countDiffLines
import buzz.delena.agentportal.ui.screens.ChatMessageItem
import buzz.delena.agentportal.ui.screens.ChatSheet
import buzz.delena.agentportal.ui.screens.ChatUiState
import buzz.delena.agentportal.ui.screens.FileChangeItem
import buzz.delena.agentportal.ui.screens.PendingPermissionItem
import buzz.delena.agentportal.ui.screens.ToolStepItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class StompAgentEvent(
    @SerialName("sessionId") val sessionId: String? = null,
    val type: String,
    val payload: JsonObject? = null,
)

class ChatViewModel(
    private val sessionId: String,
    initialTitle: String,
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val stompClient: StompWebSocketClient,
    private val appContext: Context,
    private val onArchived: () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatUiState(
            sessionTitle = initialTitle,
            connectionStatus = ConnectionStatusUi.from(authRepository.authSessionInfo()),
        ),
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamingMessageId: String? = null
    private val streamingBuffer = StringBuilder()

    init {
        viewModelScope.launch {
            sessionRepository.observeMessages(sessionId).collect { entities ->
                val roomItems = entities.map { it.toItem() }
                val streamingId = streamingMessageId
                val merged = if (streamingId != null && streamingBuffer.isNotEmpty()) {
                    val withoutStaleStream = roomItems.filterNot { it.id == streamingId }
                    withoutStaleStream + ChatMessageItem(
                        id = streamingId,
                        isUser = false,
                        contentMarkdown = streamingBuffer.toString(),
                        timeLabel = "now",
                    )
                } else {
                    roomItems
                }
                _state.value = _state.value.copy(messages = merged)
                refreshActivity(messages = merged)
            }
        }
        refresh()

        stompClient.connect()
        viewModelScope.launch {
            stompClient.connectionState.collect { connectionState ->
                _state.value = _state.value.copy(
                    realtimeState = connectionState,
                    connectionStatus = ConnectionStatusUi.from(
                        authRepository.authSessionInfo(),
                        connectionState,
                    ),
                )
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                _state.value = _state.value.copy(
                    connectionStatus = ConnectionStatusUi.from(
                        authRepository.authSessionInfo(),
                        _state.value.realtimeState,
                    ),
                )
            }
        }
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            stompClient.connectionState
                .flatMapLatest { connectionState ->
                    if (connectionState == ConnectionState.CONNECTED) {
                        stompClient.subscribeToSession(sessionId)
                    } else {
                        emptyFlow()
                    }
                }
                .collect { event ->
                    handleStompEvent(event.bodyJson)
                }
        }
    }

    private fun handleStompEvent(bodyJson: String) {
        val event = runCatching {
            NetworkModule.json.decodeFromString(StompAgentEvent.serializer(), bodyJson)
        }.getOrNull()

        if (event == null) {
            refresh()
            return
        }

        when (event.type) {
            "assistant_delta" -> appendStreamingDelta(event.payload)
            "thinking_delta" -> Unit
            "permission_required", "plan_required" -> {
                streamingMessageId = null
                streamingBuffer.setLength(0)
                refresh()
                openDecisionSheet()
            }
            "tool_call", "run_completed", "run_failed", "run_cancelled", "assistant_message" -> {
                streamingMessageId = null
                streamingBuffer.setLength(0)
                refresh()
            }
            else -> {
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

        _state.value = _state.value.copy(isSending = true, promptDraft = "", error = null)
        viewModelScope.launch {
            sessionRepository.sendPrompt(sessionId, prompt)
                .onSuccess {
                    _state.value = _state.value.copy(isSending = false)
                    refreshActivity()
                    refreshPermissions()
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        promptDraft = prompt,
                        error = userFacingErrorMessage(t),
                    )
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun openDecisionSheet() {
        if (_state.value.pendingPermission != null) {
            _state.value = _state.value.copy(activeSheet = ChatSheet.Decision)
        }
    }

    fun openToolsSheet() {
        _state.value = _state.value.copy(activeSheet = ChatSheet.Tools)
        refreshActivity()
    }

    fun openChangesSheet() {
        _state.value = _state.value.copy(activeSheet = ChatSheet.Changes)
        refreshActivity()
    }

    fun dismissSheet() {
        val backToTools = _state.value.activeSheet == ChatSheet.ToolDetail
        _state.value = _state.value.copy(
            activeSheet = if (backToTools) ChatSheet.Tools else ChatSheet.None,
            selectedTool = if (backToTools) null else _state.value.selectedTool,
        )
    }

    fun selectTool(step: ToolStepItem) {
        _state.value = _state.value.copy(
            selectedTool = step,
            activeSheet = ChatSheet.ToolDetail,
        )
    }

    fun selectChange(change: FileChangeItem) {
        viewModelScope.launch {
            val enriched = sessionRepository.getChangeDiff(sessionId, change.path)
                .getOrNull()
                ?.toItem()
                ?: change
            _state.value = _state.value.copy(selectedChange = enriched)
        }
    }

    fun acceptChange(path: String) {
        viewModelScope.launch {
            sessionRepository.acceptChange(sessionId, path)
                .onSuccess { refreshActivity() }
                .onFailure { t -> _state.value = _state.value.copy(error = userFacingErrorMessage(t)) }
        }
    }

    fun rejectChange(path: String) {
        viewModelScope.launch {
            sessionRepository.rejectChange(sessionId, path)
                .onSuccess { refreshActivity() }
                .onFailure { t -> _state.value = _state.value.copy(error = userFacingErrorMessage(t)) }
        }
    }

    fun allowOnce(permissionId: String) = decidePermission(permissionId, PermissionStatus.ALLOW_ONCE.name)
    fun allowAlways(permissionId: String) = decidePermission(permissionId, PermissionStatus.ALLOW_ALWAYS.name)
    fun reject(permissionId: String) = decidePermission(permissionId, PermissionStatus.REJECT_ONCE.name)
    fun acceptPlan(permissionId: String) = decidePermission(permissionId, "accept")
    fun rejectPlan(permissionId: String) = decidePermission(permissionId, "reject")

    fun cancelRun() {
        viewModelScope.launch {
            sessionRepository.cancelSession(sessionId)
                .onSuccess { refresh() }
                .onFailure { t -> _state.value = _state.value.copy(error = userFacingErrorMessage(t)) }
        }
    }

    fun archive() {
        viewModelScope.launch {
            sessionRepository.archiveSession(sessionId)
                .onSuccess { onArchived() }
                .onFailure { t -> _state.value = _state.value.copy(error = userFacingErrorMessage(t)) }
        }
    }

    private fun decidePermission(permissionId: String, decision: String) {
        viewModelScope.launch {
            sessionRepository.decidePermission(sessionId, permissionId, decision, reason = null)
                .onSuccess {
                    _state.value = _state.value.copy(
                        pendingPermission = null,
                        activeSheet = ChatSheet.None,
                        error = null,
                    )
                    PermissionApprovalNotifier.cancelPermissionNotification(appContext, permissionId)
                    refresh()
                }
                .onFailure { t ->
                    _state.value = _state.value.copy(error = userFacingErrorMessage(t))
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            sessionRepository.getSession(sessionId).onSuccess { session ->
                _state.value = _state.value.copy(
                    sessionTitle = session.title?.takeIf { it.isNotBlank() }
                        ?: session.workspacePath.substringAfterLast('/'),
                    sessionStatus = session.status.name,
                )
            }
            runCatching { sessionRepository.refreshMessages(sessionId) }
            refreshActivity()
            refreshPermissions()
        }
    }

    private fun refreshActivity(messages: List<ChatMessageItem> = _state.value.messages) {
        viewModelScope.launch {
            val rawTools = sessionRepository.getTools(sessionId).getOrDefault(emptyList())
            val changes = sessionRepository.getChanges(sessionId).getOrDefault(emptyList()).map { it.toItem() }
            val selectedPath = _state.value.selectedChange?.path
            val activity = ToolActivity.buildTurnActivity(
                allTools = rawTools,
                messages = messages,
                showReads = _state.value.showReadsInTimeline,
            )
            _state.value = _state.value.copy(
                tools = activity.steps,
                readTools = activity.reads,
                activityChips = activity.chipLabels,
                turnScoped = activity.turnScoped,
                sessionRawToolCount = activity.sessionRawCount,
                changes = changes,
                selectedChange = changes.firstOrNull { it.path == selectedPath } ?: changes.firstOrNull(),
            )
        }
    }

    fun openReadsSheet() {
        _state.value = _state.value.copy(
            showReadsInTimeline = true,
            activeSheet = ChatSheet.Tools,
        )
        refreshActivity()
    }

    fun toggleShowReads() {
        val next = !_state.value.showReadsInTimeline
        _state.value = _state.value.copy(
            showReadsInTimeline = next,
            activeSheet = ChatSheet.Tools,
        )
        refreshActivity()
    }

    private suspend fun refreshPermissions() {
        sessionRepository.getPendingPermissions(sessionId).onSuccess { permissions ->
            val pending = permissions.firstOrNull { it.status == PermissionStatus.PENDING }
            val previousId = _state.value.pendingPermission?.id
            val item = pending?.let {
                val isPlan = it.kind.equals("plan", ignoreCase = true)
                PendingPermissionItem(
                    id = it.id,
                    kind = it.kind,
                    toolLabel = when {
                        isPlan -> "Plan"
                        !it.toolCallId.isNullOrBlank() -> "Tool: ${it.toolCallId}"
                        else -> it.kind ?: "Tool permission"
                    },
                    detail = it.detailsJson,
                    planMarkdown = it.planMarkdown,
                )
            }
            _state.value = _state.value.copy(
                pendingPermission = item,
                activeSheet = if (item != null && item.id != previousId) {
                    ChatSheet.Decision
                } else {
                    _state.value.activeSheet
                },
            )
            if (pending != null && pending.id != previousId) {
                PermissionApprovalNotifier.postPermissionNotification(
                    context = appContext,
                    sessionId = sessionId,
                    permissionId = pending.id,
                    toolLabel = item?.toolLabel ?: "Tool permission",
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

    private fun FileChangeDto.toItem(): FileChangeItem {
        val (added, removed) = countDiffLines(unifiedDiff)
        return FileChangeItem(
            path = path,
            status = status,
            unifiedDiff = unifiedDiff,
            addedLines = added,
            removedLines = removed,
        )
    }

    class Factory(
        private val sessionId: String,
        private val initialTitle: String,
        private val sessionRepository: SessionRepository,
        private val authRepository: AuthRepository,
        private val stompClient: StompWebSocketClient,
        private val appContext: Context,
        private val onArchived: () -> Unit = {},
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(
                sessionId,
                initialTitle,
                sessionRepository,
                authRepository,
                stompClient,
                appContext,
                onArchived,
            ) as T
        }
    }
}
