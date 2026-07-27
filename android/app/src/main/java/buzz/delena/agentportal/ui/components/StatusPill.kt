package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

/**
 * Small colored pill used on session cards to summarize a session's status
 * at a glance. Colors are chosen semantically, not per literal status name,
 * so new backend statuses degrade gracefully into the muted bucket.
 */
enum class StatusTone {
    Positive,
    Attention,
    Negative,
    Muted,
}

private val activeStatuses = setOf("STREAMING", "IDLE", "RUNNING", "ACTIVE")
private val attentionStatuses = setOf("WAITING_PERMISSION", "WAITING_PLAN", "PAUSED")
private val negativeStatuses = setOf("FAILED", "ERROR")

fun statusToneFor(status: String): StatusTone = when (status.uppercase()) {
    in activeStatuses -> StatusTone.Positive
    in attentionStatuses -> StatusTone.Attention
    in negativeStatuses -> StatusTone.Negative
    else -> StatusTone.Muted
}

/** Human-facing status label for the supervisor happy path. */
fun friendlyStatusLabel(status: String): String = when (status.uppercase()) {
    "WAITING_PERMISSION", "WAITING_PLAN" -> "Needs you"
    "STREAMING", "RUNNING", "ACTIVE" -> "Running"
    "IDLE" -> "Idle"
    "FAILED", "ERROR" -> "Failed"
    "COMPLETED" -> "Done"
    "CANCELLED" -> "Cancelled"
    "ARCHIVED" -> "Archived"
    else -> status
}

fun isNeedsYouStatus(status: String): Boolean =
    status.uppercase() in attentionStatuses

fun isRunningStatus(status: String): Boolean =
    status.uppercase() in setOf("STREAMING", "RUNNING", "ACTIVE")

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val color = when (tone) {
        StatusTone.Positive -> ApColors.Success
        StatusTone.Attention -> ApColors.Warning
        StatusTone.Negative -> ApColors.Danger
        StatusTone.Muted -> ApColors.TextMuted
    }
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
        modifier = modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusPillPreview() {
    AgentPortalTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            StatusPill(label = "STREAMING", tone = StatusTone.Positive)
            StatusPill(label = "WAITING_PERMISSION", tone = StatusTone.Attention)
            StatusPill(label = "FAILED", tone = StatusTone.Negative)
            StatusPill(label = "ARCHIVED", tone = StatusTone.Muted)
        }
    }
}
