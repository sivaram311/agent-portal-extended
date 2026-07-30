package buzz.delena.agentportal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.components.ActivityTimelineSheet
import buzz.delena.agentportal.ui.components.ChangesDiffSheet
import buzz.delena.agentportal.ui.components.ChatInputBar
import buzz.delena.agentportal.ui.components.ConnectionAccountSheet
import buzz.delena.agentportal.ui.components.ConnectionStatusBar
import buzz.delena.agentportal.ui.components.MessageBubble
import buzz.delena.agentportal.ui.components.StatusPill
import buzz.delena.agentportal.ui.components.SubagentsSheet
import buzz.delena.agentportal.ui.components.ToolDetailSheet
import buzz.delena.agentportal.ui.components.TurnActivitySummary
import buzz.delena.agentportal.ui.components.friendlyStatusLabel
import buzz.delena.agentportal.ui.components.isNeedsYouStatus
import buzz.delena.agentportal.ui.components.statusToneFor

data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val contentMarkdown: String,
    val timeLabel: String,
)

enum class ToolCategory {
    READ,
    EDIT,
    SHELL,
    OTHER,
}

data class ToolStepItem(
    val id: String,
    val title: String,
    val status: String?,
    val subtitle: String? = null,
    val output: String? = null,
    val kind: String? = null,
    val category: ToolCategory = ToolCategory.OTHER,
    val startedAt: String? = null,
    val toolCallId: String? = null,
)

data class FileChangeItem(
    val path: String,
    val status: String?,
    val unifiedDiff: String?,
    val addedLines: Int,
    val removedLines: Int,
)

data class PendingPermissionItem(
    val id: String,
    val kind: String?,
    val toolLabel: String,
    val detail: String? = null,
    val planMarkdown: String? = null,
) {
    val isPlan: Boolean get() = kind.equals("plan", ignoreCase = true)
}

enum class ChatSheet {
    None,
    Decision,
    Tools,
    ToolDetail,
    Changes,
    Subagents,
}

data class ChatUiState(
    val sessionTitle: String = "",
    val sessionStatus: String = "",
    val messages: List<ChatMessageItem> = emptyList(),
    val tools: List<ToolStepItem> = emptyList(),
    val readTools: List<ToolStepItem> = emptyList(),
    val subagents: List<buzz.delena.agentportal.ui.activity.SubagentItem> = emptyList(),
    val showFinishedSubagents: Boolean = false,
    val isAbandoningSubagent: Boolean = false,
    val activityChips: List<buzz.delena.agentportal.ui.activity.ActivityChipLabel> = emptyList(),
    val showReadsInTimeline: Boolean = false,
    val turnScoped: Boolean = true,
    val sessionRawToolCount: Int = 0,
    val changes: List<FileChangeItem> = emptyList(),
    val selectedChange: FileChangeItem? = null,
    val selectedTool: ToolStepItem? = null,
    val promptDraft: String = "",
    val isSending: Boolean = false,
    val pendingPermission: PendingPermissionItem? = null,
    val activeSheet: ChatSheet = ChatSheet.None,
    val error: String? = null,
    val isReconnecting: Boolean = false,
    val realtimeState: buzz.delena.agentportal.core.network.ConnectionState =
        buzz.delena.agentportal.core.network.ConnectionState.DISCONNECTED,
    val connectionStatus: buzz.delena.agentportal.ui.components.ConnectionStatusUi? = null,
) {
    val changeCount get() = changes.size
    val activeSubagentCount get() = subagents.count { it.active }
    val failedToolCount get() = tools.count {
        it.status.equals("failed", true) || it.status.equals("error", true)
    }
    val runningToolCount get() = tools.count {
        it.status.equals("running", true) || it.status.equals("in_progress", true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onOpenDecisionSheet: () -> Unit,
    onDismissSheet: () -> Unit,
    onOpenToolsSheet: () -> Unit,
    onOpenChangesSheet: () -> Unit,
    onOpenSubagentsSheet: () -> Unit = {},
    onToggleFinishedSubagents: () -> Unit = {},
    onAbandonSubagent: (String) -> Unit = {},
    onOpenReadsSheet: () -> Unit,
    onToggleShowReads: () -> Unit,
    onSelectTool: (ToolStepItem) -> Unit,
    onSelectChange: (FileChangeItem) -> Unit,
    onAcceptChange: (String) -> Unit,
    onRejectChange: (String) -> Unit,
    onAllowOnce: (String) -> Unit,
    onAllowAlways: (String) -> Unit,
    onReject: (String) -> Unit,
    onAcceptPlan: (String) -> Unit,
    onRejectPlan: (String) -> Unit,
    onCancelRun: () -> Unit,
    onArchive: () -> Unit,
    onDismissError: () -> Unit,
    onReconnect: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var showConnectionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size, state.activityChips.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex.coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = ApColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.sessionTitle.ifBlank { "Session" },
                            color = ApColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.sessionStatus.isNotBlank()) {
                            Text(
                                text = friendlyStatusLabel(state.sessionStatus),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isNeedsYouStatus(state.sessionStatus)) {
                                    ApColors.Warning
                                } else {
                                    ApColors.TextMuted
                                },
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ApColors.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = ApColors.TextPrimary,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Activity") },
                            onClick = {
                                menuOpen = false
                                onOpenToolsSheet()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.activeSubagentCount > 0) {
                                        "Sub-agents (${state.activeSubagentCount})"
                                    } else {
                                        "Sub-agents"
                                    },
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onOpenSubagentsSheet()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Changes") },
                            onClick = {
                                menuOpen = false
                                onOpenChangesSheet()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Cancel run") },
                            onClick = {
                                menuOpen = false
                                onCancelRun()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApColors.Background,
                    titleContentColor = ApColors.TextPrimary,
                ),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                if (state.error != null) {
                    ErrorBanner(message = state.error, onDismiss = onDismissError)
                }
                if (state.pendingPermission != null && state.activeSheet != ChatSheet.Decision) {
                    NeedsYouBanner(
                        label = if (state.pendingPermission.isPlan) {
                            "Plan needs approval"
                        } else {
                            "Permission requested"
                        },
                        onReview = onOpenDecisionSheet,
                    )
                }
                ChatInputBar(
                    value = state.promptDraft,
                    onValueChange = onPromptChange,
                    onSend = onSendPrompt,
                    isSending = state.isSending,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.connectionStatus?.let { status ->
                ConnectionStatusBar(
                    status = status,
                    onOpenDetails = { showConnectionSheet = true },
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MessageBubble(
                            isUser = message.isUser,
                            contentMarkdown = message.contentMarkdown,
                            timeLabel = message.timeLabel,
                        )
                        // Attach activity chips under the latest assistant turn only.
                        val isLatestAssistant = !message.isUser && message.id == state.messages.lastOrNull { !it.isUser }?.id
                        if (isLatestAssistant) {
                            TurnActivitySummary(
                                chips = state.activityChips,
                                changeCount = state.changeCount,
                                onOpenTools = onOpenToolsSheet,
                                onOpenReads = onOpenReadsSheet,
                                onOpenChanges = onOpenChangesSheet,
                                onOpenSubagents = onOpenSubagentsSheet,
                                modifier = Modifier.padding(end = 48.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    when (state.activeSheet) {
        ChatSheet.Decision -> if (state.pendingPermission != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismissSheet,
                sheetState = sheetState,
                containerColor = ApColors.Surface,
            ) {
                DecisionSheetContent(
                    permission = state.pendingPermission,
                    onAllowOnce = { onAllowOnce(state.pendingPermission.id) },
                    onAllowAlways = { onAllowAlways(state.pendingPermission.id) },
                    onReject = { onReject(state.pendingPermission.id) },
                    onAcceptPlan = { onAcceptPlan(state.pendingPermission.id) },
                    onRejectPlan = { onRejectPlan(state.pendingPermission.id) },
                )
            }
        }
        ChatSheet.Tools -> ActivityTimelineSheet(
            steps = state.tools,
            reads = state.readTools,
            showReads = state.showReadsInTimeline,
            turnScoped = state.turnScoped,
            sessionRawCount = state.sessionRawToolCount,
            onToggleReads = onToggleShowReads,
            onDismiss = onDismissSheet,
            onOpenStepDetail = onSelectTool,
        )
        ChatSheet.ToolDetail -> if (state.selectedTool != null) {
            ToolDetailSheet(step = state.selectedTool, onDismiss = onDismissSheet)
        }
        ChatSheet.Changes -> ChangesDiffSheet(
            changes = state.changes,
            selected = state.selectedChange,
            onSelect = onSelectChange,
            onAccept = onAcceptChange,
            onReject = onRejectChange,
            onDismiss = onDismissSheet,
        )
        ChatSheet.Subagents -> SubagentsSheet(
            subagents = state.subagents,
            showFinished = state.showFinishedSubagents,
            isBusy = state.isAbandoningSubagent,
            onToggleFinished = onToggleFinishedSubagents,
            onAbandon = onAbandonSubagent,
            onDismiss = onDismissSheet,
        )
        ChatSheet.None -> Unit
    }

    if (showConnectionSheet && state.connectionStatus != null) {
        ConnectionAccountSheet(
            status = state.connectionStatus,
            isReconnecting = state.isReconnecting,
            onReconnect = onReconnect,
            onSignOut = {
                showConnectionSheet = false
                onSignOut()
            },
            onDismiss = { showConnectionSheet = false },
        )
    }
}

@Composable
private fun NeedsYouBanner(label: String, onReview: () -> Unit) {
    Surface(
        color = ApColors.Warning.copy(alpha = 0.14f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = ApColors.Warning,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = label,
                color = ApColors.Warning,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReview) {
                Text("Review", color = ApColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DecisionSheetContent(
    permission: PendingPermissionItem,
    onAllowOnce: () -> Unit,
    onAllowAlways: () -> Unit,
    onReject: () -> Unit,
    onAcceptPlan: () -> Unit,
    onRejectPlan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (permission.isPlan) "Plan approval" else "Permission requested",
                style = MaterialTheme.typography.titleLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            StatusPill(label = "Needs you", tone = statusToneFor("WAITING_PERMISSION"))
        }
        Text(
            text = permission.toolLabel,
            style = MaterialTheme.typography.titleMedium,
            color = ApColors.TextPrimary,
        )
        val body = permission.planMarkdown ?: permission.detail
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            )
        }
        if (permission.isPlan) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRejectPlan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.Danger),
                    border = BorderStroke(1.dp, ApColors.Danger),
                ) { Text("Reject") }
                Button(
                    onClick = onAcceptPlan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApColors.Accent,
                        contentColor = ApColors.Background,
                    ),
                ) { Text("Accept", fontWeight = FontWeight.SemiBold) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAllowOnce,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApColors.Accent,
                        contentColor = ApColors.Background,
                    ),
                ) { Text("Allow once", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = onAllowAlways,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.Accent),
                    border = BorderStroke(1.dp, ApColors.Accent),
                ) { Text("Always allow") }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.Danger),
                    border = BorderStroke(1.dp, ApColors.Danger),
                ) { Text("Reject") }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = ApColors.Danger,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text("Dismiss", color = ApColors.TextMuted)
        }
    }
}
