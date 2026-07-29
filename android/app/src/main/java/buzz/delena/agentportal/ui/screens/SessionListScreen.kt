package buzz.delena.agentportal.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.components.ConnectionAccountSheet
import buzz.delena.agentportal.ui.components.ConnectionStatusBar
import buzz.delena.agentportal.ui.components.ConnectionStatusUi
import buzz.delena.agentportal.ui.components.SessionCard
import buzz.delena.agentportal.ui.components.SessionCardData
import buzz.delena.agentportal.ui.viewmodel.SessionListFilter

/** One row in the session list. Kept UI-layer-only; the parent's ViewModel maps its domain model to this. */
data class SessionListItem(
    val id: String,
    val title: String,
    val workspacePath: String,
    val status: String,
    val provider: String?,
    val updatedAtLabel: String,
)

data class SessionListUiState(
    val sessions: List<SessionListItem> = emptyList(),
    val filter: SessionListFilter = SessionListFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCreating: Boolean = false,
    val isReconnecting: Boolean = false,
    val error: String? = null,
    val connectionStatus: ConnectionStatusUi? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    state: SessionListUiState,
    onSessionClick: (String) -> Unit,
    onCreateSession: (provider: String) -> Unit,
    onFilterChange: (SessionListFilter) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onReconnect: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var showConnectionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = ApColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sessions",
                        color = ApColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ApColors.Background,
                    titleContentColor = ApColors.TextPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = ApColors.Accent,
                contentColor = ApColors.Background,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "New session")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.connectionStatus?.let { status ->
                    ConnectionStatusBar(
                        status = status,
                        onOpenDetails = { showConnectionSheet = true },
                    )
                }
                FilterRow(
                    selected = state.filter,
                    onFilterChange = onFilterChange,
                )

                if (state.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.error,
                            color = ApColors.Danger,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss", color = ApColors.TextMuted)
                        }
                    }
                }

                when {
                    state.isLoading && state.sessions.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ApColors.Accent)
                        }
                    }

                    state.sessions.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = emptyTitle(state.filter),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ApColors.TextPrimary,
                                )
                                Text(
                                    text = emptySubtitle(state.filter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ApColors.TextMuted,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.sessions, key = { it.id }) { session ->
                                SessionCard(
                                    session = SessionCardData(
                                        id = session.id,
                                        title = session.title,
                                        workspacePath = session.workspacePath,
                                        status = session.status,
                                        provider = session.provider,
                                        updatedAtLabel = session.updatedAtLabel,
                                    ),
                                    onClick = { onSessionClick(session.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { if (!state.isCreating) showCreateSheet = false },
            sheetState = sheetState,
            containerColor = ApColors.Surface,
        ) {
            CreateSessionSheet(
                isCreating = state.isCreating,
                onCreate = { provider ->
                    onCreateSession(provider)
                    showCreateSheet = false
                },
                onCancel = { showCreateSheet = false },
            )
        }
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
private fun FilterRow(
    selected: SessionListFilter,
    onFilterChange: (SessionListFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionListFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ApColors.Accent.copy(alpha = 0.22f),
                    selectedLabelColor = ApColors.Accent,
                    containerColor = ApColors.Surface,
                    labelColor = ApColors.TextMuted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == filter,
                    borderColor = ApColors.SurfaceAlt,
                    selectedBorderColor = ApColors.Accent,
                ),
            )
        }
    }
}

@Composable
private fun CreateSessionSheet(
    isCreating: Boolean,
    onCreate: (provider: String) -> Unit,
    onCancel: () -> Unit,
) {
    var provider by remember { mutableStateOf("cursor") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "New session",
            style = MaterialTheme.typography.titleLarge,
            color = ApColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Thin create — workspace defaults to demo. Full picker comes later.",
            style = MaterialTheme.typography.bodyMedium,
            color = ApColors.TextMuted,
        )

        Text(
            text = "Provider",
            style = MaterialTheme.typography.labelLarge,
            color = ApColors.TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = provider == "cursor",
                onClick = { provider = "cursor" },
                label = { Text("Cursor") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ApColors.Accent.copy(alpha = 0.22f),
                    selectedLabelColor = ApColors.Accent,
                ),
            )
            FilterChip(
                selected = provider == "antigravity",
                onClick = { provider = "antigravity" },
                label = { Text("Antigravity") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ApColors.Accent.copy(alpha = 0.22f),
                    selectedLabelColor = ApColors.Accent,
                ),
            )
        }

        Text(
            text = "Workspace: demo",
            style = MaterialTheme.typography.bodyMedium,
            color = ApColors.TextPrimary,
        )

        Button(
            onClick = { onCreate(provider) },
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ApColors.Accent,
                contentColor = ApColors.Background,
            ),
        ) {
            Text(if (isCreating) "Starting…" else "Start", fontWeight = FontWeight.SemiBold)
        }
        TextButton(
            onClick = onCancel,
            enabled = !isCreating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel", color = ApColors.TextMuted)
        }
    }
}

private fun emptyTitle(filter: SessionListFilter): String = when (filter) {
    SessionListFilter.NEEDS_YOU -> "Nothing needs you"
    SessionListFilter.RUNNING -> "No running sessions"
    SessionListFilter.FAILED -> "No failed sessions"
    SessionListFilter.ALL -> "No sessions yet"
}

private fun emptySubtitle(filter: SessionListFilter): String = when (filter) {
    SessionListFilter.ALL -> "Tap + to start one"
    else -> "Pull to refresh, or switch filter"
}

private val previewSessions = listOf(
    SessionListItem(
        id = "1",
        title = "Refactor auth module",
        workspacePath = "/home/dev/projects/agent-portal/backend",
        status = "STREAMING",
        provider = "cursor",
        updatedAtLabel = "2m ago",
    ),
    SessionListItem(
        id = "2",
        title = "Investigate flaky E2E test",
        workspacePath = "/home/dev/projects/agent-portal/frontend/e2e",
        status = "WAITING_PERMISSION",
        provider = "antigravity",
        updatedAtLabel = "just now",
    ),
    SessionListItem(
        id = "3",
        title = "Nightly dependency bump",
        workspacePath = "/home/dev/projects/agent-portal",
        status = "FAILED",
        provider = "cursor",
        updatedAtLabel = "1h ago",
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SessionListScreenPreview() {
    AgentPortalTheme {
        SessionListScreen(
            state = SessionListUiState(sessions = previewSessions),
            onSessionClick = {},
            onCreateSession = {},
            onFilterChange = {},
            onRefresh = {},
            onDismissError = {},
        )
    }
}
