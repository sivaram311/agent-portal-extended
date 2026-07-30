package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
    val refreshStateLabel: String,
    val hasRefreshToken: Boolean,
    val authServerHost: String?,
    val clientId: String?,
    val needsSignIn: Boolean,
    val needsReconnect: Boolean,
    val canReconnect: Boolean,
    val realtimeLabel: String? = null,
    val realtimeTone: StatusTone = StatusTone.Muted,
) {
    val tone: StatusTone
        get() = when {
            needsSignIn -> StatusTone.Negative
            needsReconnect || realtimeTone == StatusTone.Negative -> StatusTone.Attention
            realtimeTone == StatusTone.Positive -> StatusTone.Positive
            else -> StatusTone.Muted
        }

    companion object {
        fun from(
            info: AuthSessionInfo,
            realtime: ConnectionState? = null,
        ): ConnectionStatusUi {
            val (label, tone) = when (realtime) {
                null -> null to StatusTone.Muted
                ConnectionState.CONNECTED -> "Live" to StatusTone.Positive
                ConnectionState.CONNECTING -> "Connecting..." to StatusTone.Attention
                ConnectionState.FAILED -> "Realtime offline" to StatusTone.Negative
                ConnectionState.DISCONNECTED -> "Realtime idle" to StatusTone.Muted
            }
            return ConnectionStatusUi(
                authMethodLabel = info.authMethod.label,
                subject = info.subject,
                tokenStateLabel = info.tokenStateLabel,
                refreshStateLabel = info.refreshStateLabel,
                hasRefreshToken = info.hasRefreshToken,
                authServerHost = info.authServerHost,
                clientId = info.clientId,
                needsSignIn = info.needsSignIn,
                needsReconnect = info.needsReconnect,
                canReconnect = info.canReconnect,
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
    onOpenDetails: (() -> Unit)? = null,
) {
    val dotColor = when (status.tone) {
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
                if (onOpenDetails != null) Modifier.clickable(onClick = onOpenDetails) else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
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
            if (onOpenDetails != null) {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelMedium,
                    color = ApColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = "${status.tokenStateLabel} · ${status.refreshStateLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = ApColors.TextMuted,
            modifier = Modifier.padding(top = 2.dp, start = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionAccountSheet(
    status: ConnectionStatusUi,
    isReconnecting: Boolean,
    onReconnect: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
    onSendDiagnostics: (() -> Unit)? = null,
    isSendingDiagnostics: Boolean = false,
    diagnosticsMessage: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ApColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            StatusRow("Sign-in", status.authMethodLabel)
            StatusRow("User", status.subject ?: "-")
            StatusRow("Access", status.tokenStateLabel)
            StatusRow("Refresh", status.refreshStateLabel)
            StatusRow("Client", status.clientId ?: "-")
            StatusRow("Auth host", status.authServerHost ?: "-")
            if (!status.realtimeLabel.isNullOrBlank()) {
                StatusRow("Realtime", status.realtimeLabel)
            }
            if (status.needsSignIn) {
                Text(
                    text = "Session cannot be recovered here - sign out and sign in again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.Danger,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else if (status.needsReconnect) {
                Text(
                    text = "Access token expired. Tap Reconnect to refresh it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.Warning,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onReconnect,
                enabled = !isReconnecting && !isSendingDiagnostics && status.canReconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApColors.Accent,
                    contentColor = ApColors.Background,
                    disabledContainerColor = ApColors.SurfaceAlt,
                    disabledContentColor = ApColors.TextMuted,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isReconnecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            color = ApColors.Background,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "  Reconnecting...",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Text("Reconnect", fontWeight = FontWeight.SemiBold)
                }
            }
            if (onSendDiagnostics != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSendDiagnostics,
                    enabled = !isReconnecting && !isSendingDiagnostics && !status.needsSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isSendingDiagnostics) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                color = ApColors.Accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("  Sending diagnostics...")
                        }
                    } else {
                        Text("Send diagnostics", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!diagnosticsMessage.isNullOrBlank()) {
                    Text(
                        text = diagnosticsMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ApColors.TextMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                enabled = !isReconnecting && !isSendingDiagnostics,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ApColors.Danger),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Sign out", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Close", color = ApColors.TextMuted)
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = ApColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            color = ApColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp),
        )
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
                refreshStateLabel = "Refresh ready",
                hasRefreshToken = true,
                authServerHost = "delena.buzz",
                clientId = "agent-portal",
                needsSignIn = false,
                needsReconnect = false,
                canReconnect = true,
                realtimeLabel = "Live",
                realtimeTone = StatusTone.Positive,
            ),
            onOpenDetails = {},
        )
    }
}
