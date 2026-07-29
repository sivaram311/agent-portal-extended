package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.core.data.AuthMethod
import buzz.delena.agentportal.core.data.AuthSessionInfo
import buzz.delena.agentportal.core.network.ConnectionState
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

data class ConnectionStatusUi(
    val authMethodLabel: String,
    val subject: String?,
    val tokenStateLabel: String,
    val hasRefreshToken: Boolean,
    val authServerHost: String?,
    val clientId: String?,
    val realtimeLabel: String? = null,
    val realtimeTone: StatusTone = StatusTone.Muted,
) {
    companion object {
        fun from(
            info: AuthSessionInfo,
            realtime: ConnectionState? = null,
        ): ConnectionStatusUi {
            val (label, tone) = when (realtime) {
                null -> null to StatusTone.Muted
                ConnectionState.CONNECTED -> "Live" to StatusTone.Positive
                ConnectionState.CONNECTING -> "Connecting…" to StatusTone.Attention
                ConnectionState.FAILED -> "Realtime offline" to StatusTone.Negative
                ConnectionState.DISCONNECTED -> "Realtime idle" to StatusTone.Muted
            }
            return ConnectionStatusUi(
                authMethodLabel = info.authMethod.label,
                subject = info.subject,
                tokenStateLabel = info.tokenStateLabel,
                hasRefreshToken = info.hasRefreshToken,
                authServerHost = info.authServerHost,
                clientId = info.clientId,
                realtimeLabel = label,
                realtimeTone = tone,
            )
        }
    }
}

@Composable
fun ConnectionStatusBar(
    status: ConnectionStatusUi,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val primaryTone = when {
        status.tokenStateLabel.contains("expired", ignoreCase = true) ||
            status.tokenStateLabel.contains("No access", ignoreCase = true) -> StatusTone.Negative
        status.realtimeTone == StatusTone.Negative -> StatusTone.Attention
        status.realtimeTone == StatusTone.Positive -> StatusTone.Positive
        else -> StatusTone.Muted
    }
    val dotColor = when (primaryTone) {
        StatusTone.Positive -> ApColors.Success
        StatusTone.Attention -> ApColors.Warning
        StatusTone.Negative -> ApColors.Danger
        StatusTone.Muted -> ApColors.TextMuted
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ApColors.Surface)
            .then(
                if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = buildString {
                    append(status.authMethodLabel)
                    if (!status.subject.isNullOrBlank()) {
                        append(" · ")
                        append(status.subject)
                    }
                    if (!status.realtimeLabel.isNullOrBlank()) {
                        append(" · ")
                        append(status.realtimeLabel)
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = status.tokenStateLabel +
                if (status.hasRefreshToken) " · refresh ready" else " · no refresh token",
            style = MaterialTheme.typography.labelMedium,
            color = ApColors.TextMuted,
            modifier = Modifier.padding(top = 2.dp, start = 16.dp),
        )
        if (expanded) {
            val detail = buildString {
                if (!status.clientId.isNullOrBlank()) append("client ").append(status.clientId)
                if (!status.authServerHost.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(status.authServerHost)
                }
                if (!status.realtimeLabel.isNullOrBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("WS ").append(status.realtimeLabel)
                }
            }
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = ApColors.TextMuted,
                    modifier = Modifier.padding(top = 2.dp, start = 16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusBarPreview() {
    AgentPortalTheme {
        ConnectionStatusBar(
            status = ConnectionStatusUi(
                authMethodLabel = AuthMethod.PASSWORD.label,
                subject = "admin",
                tokenStateLabel = "Token · 12m left",
                hasRefreshToken = true,
                authServerHost = "delena.buzz",
                clientId = "agent-portal",
                realtimeLabel = "Live",
                realtimeTone = StatusTone.Positive,
            ),
            expanded = true,
        )
    }
}
