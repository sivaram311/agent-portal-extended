package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

/** Row model for [SessionCard]; kept UI-layer-only so this file has no data/network dependency. */
data class SessionCardData(
    val id: String,
    val title: String,
    val workspacePath: String,
    val status: String,
    val provider: String?,
    val updatedAtLabel: String,
)

@Composable
fun SessionCard(
    session: SessionCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ApColors.Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ApColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(
                    label = friendlyStatusLabel(session.status),
                    tone = statusToneFor(session.status),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = session.workspacePath,
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (session.provider?.contains("cursor", ignoreCase = true) == true) {
                        Icons.Filled.Terminal
                    } else {
                        Icons.Filled.Bolt
                    },
                    contentDescription = null,
                    tint = ApColors.TextMuted,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = session.provider ?: "unknown provider",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextMuted,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextMuted,
                )
                Text(
                    text = session.updatedAtLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextMuted,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun SessionCardPreview() {
    AgentPortalTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            SessionCard(
                session = SessionCardData(
                    id = "1",
                    title = "Refactor auth module",
                    workspacePath = "/home/dev/projects/agent-portal/backend",
                    status = "STREAMING",
                    provider = "Cursor CLI",
                    updatedAtLabel = "2m ago",
                ),
                onClick = {},
            )
            SessionCard(
                session = SessionCardData(
                    id = "2",
                    title = "Investigate flaky E2E test",
                    workspacePath = "/home/dev/projects/agent-portal/frontend/e2e",
                    status = "WAITING_PERMISSION",
                    provider = "Antigravity CLI",
                    updatedAtLabel = "just now",
                ),
                onClick = {},
            )
            SessionCard(
                session = SessionCardData(
                    id = "3",
                    title = "Nightly dependency bump",
                    workspacePath = "/home/dev/projects/agent-portal",
                    status = "FAILED",
                    provider = "Cursor CLI",
                    updatedAtLabel = "1h ago",
                ),
                onClick = {},
            )
            SessionCard(
                session = SessionCardData(
                    id = "4",
                    title = "Archive old scratch session",
                    workspacePath = "/home/dev/scratch",
                    status = "ARCHIVED",
                    provider = null,
                    updatedAtLabel = "3d ago",
                ),
                onClick = {},
            )
        }
    }
}
