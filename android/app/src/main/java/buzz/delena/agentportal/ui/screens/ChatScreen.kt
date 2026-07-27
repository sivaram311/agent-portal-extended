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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.components.ChatInputBar
import buzz.delena.agentportal.ui.components.MessageBubble

/** One rendered chat message. Kept UI-layer-only; the parent's ViewModel maps its domain model to this. */
data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val contentMarkdown: String,
    val timeLabel: String,
)

/** A tool-use permission request the agent is blocked on, awaiting the human's approve/reject. */
data class PendingPermissionItem(
    val id: String,
    val toolLabel: String,
    val detail: String? = null,
)

data class ChatUiState(
    val sessionTitle: String = "",
    val messages: List<ChatMessageItem> = emptyList(),
    val promptDraft: String = "",
    val isSending: Boolean = false,
    val pendingPermission: PendingPermissionItem? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onApprovePermission: (String) -> Unit,
    onRejectPermission: (String) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = ApColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.sessionTitle.ifBlank { "Session" },
                        color = ApColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApColors.Background,
                    titleContentColor = ApColors.TextPrimary,
                ),
            )
        },
        bottomBar = {
            // Edge-to-edge (enableEdgeToEdge() in MainActivity) means the
            // activity's own decor no longer resizes for the IME the way
            // android:windowSoftInputMode="adjustResize" alone implies pre-Compose
            // -- without an explicit imePadding() here, this bar stays fixed at
            // the bottom of the screen and the keyboard draws over it instead of
            // pushing it up.
            Column(modifier = Modifier.imePadding()) {
                if (state.pendingPermission != null) {
                    PermissionRequestCard(
                        permission = state.pendingPermission,
                        onApprove = { onApprovePermission(state.pendingPermission.id) },
                        onReject = { onRejectPermission(state.pendingPermission.id) },
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(
                    isUser = message.isUser,
                    contentMarkdown = message.contentMarkdown,
                    timeLabel = message.timeLabel,
                )
            }
        }
    }
}

/**
 * The remote tool-approval card. This is the mobile counterpart of Agent Portal's web
 * permission dialog and the whole point of supervising a session from a phone, so it is
 * deliberately loud (warning-tinted border/icon) and offers no dismiss gesture -- the human
 * must explicitly approve or reject before the agent can continue.
 */
@Composable
private fun PermissionRequestCard(
    permission: PendingPermissionItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ApColors.SurfaceAlt),
        border = BorderStroke(1.5.dp, ApColors.Warning),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = ApColors.Warning,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = "Permission requested",
                    style = MaterialTheme.typography.titleMedium,
                    color = ApColors.Warning,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = null,
                    tint = ApColors.TextMuted,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = permission.toolLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ApColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (permission.detail != null) {
                Text(
                    text = permission.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.Danger),
                    border = BorderStroke(1.dp, ApColors.Danger),
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApColors.Warning,
                        contentColor = ApColors.Background,
                    ),
                ) {
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private val previewMessages = listOf(
    ChatMessageItem(
        id = "1",
        isUser = true,
        contentMarkdown = "Can you add a health-check endpoint?",
        timeLabel = "10:00 AM",
    ),
    ChatMessageItem(
        id = "2",
        isUser = false,
        contentMarkdown = "Sure — adding `GET /healthz` returning `200 OK`.\n\n```kotlin\n@GetMapping(\"/healthz\")\nfun health() = \"OK\"\n```",
        timeLabel = "10:00 AM",
    ),
    ChatMessageItem(
        id = "3",
        isUser = true,
        contentMarkdown = "Looks good, go ahead and run the tests.",
        timeLabel = "10:01 AM",
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun ChatScreenPreview() {
    AgentPortalTheme {
        ChatScreen(
            state = ChatUiState(
                sessionTitle = "Add health-check endpoint",
                messages = previewMessages,
                promptDraft = "",
                isSending = false,
            ),
            onPromptChange = {},
            onSendPrompt = {},
            onApprovePermission = {},
            onRejectPermission = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Pending permission")
@Composable
private fun ChatScreenPermissionPreview() {
    AgentPortalTheme {
        ChatScreen(
            state = ChatUiState(
                sessionTitle = "Add health-check endpoint",
                messages = previewMessages,
                promptDraft = "",
                isSending = false,
                pendingPermission = PendingPermissionItem(
                    id = "perm-1",
                    toolLabel = "Run shell command",
                    detail = "rm -rf build/ && ./gradlew test",
                ),
            ),
            onPromptChange = {},
            onSendPrompt = {},
            onApprovePermission = {},
            onRejectPermission = {},
            onBack = {},
        )
    }
}
