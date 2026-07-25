package buzz.delena.agentportal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.components.SessionCard
import buzz.delena.agentportal.ui.components.SessionCardData

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
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    state: SessionListUiState,
    onSessionClick: (String) -> Unit,
    onCreateSession: () -> Unit,
    onRefresh: () -> Unit,
) {
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
                onClick = onCreateSession,
                containerColor = ApColors.Accent,
                contentColor = ApColors.Background,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "New session")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ApColors.Accent,
                    trackColor = ApColors.SurfaceAlt,
                )
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
                                text = "No sessions yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = ApColors.TextPrimary,
                            )
                            Text(
                                text = "Tap + to start one",
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

private val previewSessions = listOf(
    SessionListItem(
        id = "1",
        title = "Refactor auth module",
        workspacePath = "/home/dev/projects/agent-portal/backend",
        status = "STREAMING",
        provider = "Cursor CLI",
        updatedAtLabel = "2m ago",
    ),
    SessionListItem(
        id = "2",
        title = "Investigate flaky E2E test",
        workspacePath = "/home/dev/projects/agent-portal/frontend/e2e",
        status = "WAITING_PERMISSION",
        provider = "Antigravity CLI",
        updatedAtLabel = "just now",
    ),
    SessionListItem(
        id = "3",
        title = "Nightly dependency bump",
        workspacePath = "/home/dev/projects/agent-portal",
        status = "FAILED",
        provider = "Cursor CLI",
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
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Empty")
@Composable
private fun SessionListScreenEmptyPreview() {
    AgentPortalTheme {
        SessionListScreen(
            state = SessionListUiState(sessions = emptyList()),
            onSessionClick = {},
            onCreateSession = {},
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Refreshing")
@Composable
private fun SessionListScreenRefreshingPreview() {
    AgentPortalTheme {
        SessionListScreen(
            state = SessionListUiState(sessions = previewSessions, isRefreshing = true),
            onSessionClick = {},
            onCreateSession = {},
            onRefresh = {},
        )
    }
}
