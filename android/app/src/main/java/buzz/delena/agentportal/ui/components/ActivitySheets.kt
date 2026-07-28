package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.screens.ToolStepItem

/**
 * Collapsible Claude-style summary under a chat turn.
 * Primary chip = edits/commands; separate reads chip; changes chip.
 */
@Composable
fun TurnActivitySummary(
    chips: List<buzz.delena.agentportal.ui.activity.ActivityChipLabel>,
    changeCount: Int,
    onOpenTools: () -> Unit,
    onOpenReads: () -> Unit,
    onOpenChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty() && changeCount <= 0) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            SummaryChip(
                label = chip.text,
                tone = when (chip.kind) {
                    buzz.delena.agentportal.ui.activity.ChipKind.READS -> ApColors.Info
                    else -> ApColors.TextMuted
                },
                onClick = {
                    when (chip.kind) {
                        buzz.delena.agentportal.ui.activity.ChipKind.READS -> onOpenReads()
                        else -> onOpenTools()
                    }
                },
            )
        }
        if (changeCount > 0) {
            SummaryChip(
                label = "$changeCount file${if (changeCount == 1) "" else "s"} changed",
                tone = ApColors.Accent,
                onClick = onOpenChanges,
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    tone: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .background(tone.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Terminal,
            contentDescription = null,
            tint = tone,
            modifier = Modifier
                .size(16.dp)
                .padding(end = 2.dp),
        )
        Text(
            text = label,
            color = tone,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTimelineSheet(
    steps: List<ToolStepItem>,
    reads: List<ToolStepItem>,
    showReads: Boolean,
    turnScoped: Boolean,
    sessionRawCount: Int,
    onToggleReads: () -> Unit,
    onDismiss: () -> Unit,
    onOpenStepDetail: (ToolStepItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commands = steps.filter {
        it.category == buzz.delena.agentportal.ui.screens.ToolCategory.SHELL ||
            it.category == buzz.delena.agentportal.ui.screens.ToolCategory.EDIT ||
            it.category == buzz.delena.agentportal.ui.screens.ToolCategory.OTHER
    }
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
                text = "Activity",
                style = MaterialTheme.typography.titleLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    append(if (turnScoped) "This turn" else "Session")
                    append(" · ${commands.size} attention")
                    if (reads.isNotEmpty()) append(" · ${reads.size} reads")
                    if (sessionRawCount > commands.size + reads.size) {
                        append(" · $sessionRawCount raw")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (commands.isNotEmpty()) {
                    SectionHeader("Commands & edits")
                    commands.forEachIndexed { index, step ->
                        TimelineNode(
                            step = step,
                            isLast = index == commands.lastIndex && (!showReads || reads.isEmpty()),
                            onClick = { onOpenStepDetail(step) },
                        )
                    }
                }

                if (reads.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleReads)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showReads) "Hide reads (${reads.size})" else "Show reads (${reads.size})",
                            color = ApColors.Info,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = ApColors.Info,
                        )
                    }
                    if (showReads) {
                        reads.forEachIndexed { index, step ->
                            TimelineNode(
                                step = step,
                                isLast = index == reads.lastIndex,
                                onClick = { onOpenStepDetail(step) },
                            )
                        }
                    }
                }

                if (commands.isEmpty() && reads.isEmpty()) {
                    Text(
                        text = "No meaningful tools in this turn.",
                        color = ApColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = ApColors.TextMuted,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
    )
}

@Composable
private fun TimelineNode(
    step: ToolStepItem,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = if (isLast) 0.dp else 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(statusColor(step.status), CircleShape),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(ApColors.Border),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 12.dp),
        ) {
            Text(
                text = step.title,
                color = ApColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    imageVector = statusIcon(step.status),
                    contentDescription = null,
                    tint = statusColor(step.status),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = friendlyToolStatus(step.status),
                    color = statusColor(step.status),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            if (!step.subtitle.isNullOrBlank()) {
                Text(
                    text = step.subtitle,
                    color = ApColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailSheet(
    step: ToolStepItem,
    onDismiss: () -> Unit,
) {
    var renderMarkdown by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val body = step.output?.ifBlank { null } ?: step.subtitle ?: "(no output)"

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
                text = step.title,
                style = MaterialTheme.typography.titleLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = friendlyToolStatus(step.status),
                color = statusColor(step.status),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { renderMarkdown = !renderMarkdown }) {
                    Text(
                        text = if (renderMarkdown) "Raw" else "Render",
                        color = ApColors.Accent,
                    )
                }
            }
            HorizontalDivider(color = ApColors.Border)
            if (renderMarkdown) {
                MarkdownBlock(text = body, modifier = Modifier.padding(top = 12.dp))
            } else {
                MonospaceCodeBlock(
                    text = body,
                    showLineNumbers = true,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .height(320.dp),
                )
            }
        }
    }
}

@Composable
fun MonospaceCodeBlock(
    text: String,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier,
) {
    val lines = remember(text) { text.lines().ifEmpty { listOf("") } }
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ApColors.Background, RoundedCornerShape(12.dp))
            .border(1.dp, ApColors.Border, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .verticalScroll(scroll),
    ) {
        if (showLineNumbers) {
            Column(modifier = Modifier.padding(end = 10.dp)) {
                lines.forEachIndexed { index, _ ->
                    Text(
                        text = index.toString(),
                        color = ApColors.TextMuted.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            lines.forEach { line ->
                Text(
                    text = line.ifEmpty { " " },
                    color = ApColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlock(text: String, modifier: Modifier = Modifier) {
    // Reuse Markwon via MessageBubble's public surface: simple Text fallback for sheet.
    Text(
        text = text,
        color = ApColors.TextPrimary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    )
}

private fun statusColor(status: String?): Color = when (status?.lowercase()) {
    "completed", "success", "ok", "done" -> ApColors.Success
    "failed", "error" -> ApColors.Danger
    "running", "pending", "in_progress" -> ApColors.Warning
    else -> ApColors.TextMuted
}

private fun statusIcon(status: String?) = when (status?.lowercase()) {
    "completed", "success", "ok", "done" -> Icons.Filled.CheckCircle
    "failed", "error" -> Icons.Filled.Error
    else -> Icons.Filled.HourglassTop
}

fun friendlyToolStatus(status: String?): String = when (status?.lowercase()) {
    "completed", "success", "ok", "done" -> "Completed"
    "failed", "error" -> "Failed"
    "running", "in_progress" -> "Running"
    "pending" -> "Pending"
    null, "" -> "Unknown"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
