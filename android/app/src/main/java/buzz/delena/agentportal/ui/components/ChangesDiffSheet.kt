package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.screens.FileChangeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangesDiffSheet(
    changes: List<FileChangeItem>,
    selected: FileChangeItem?,
    onSelect: (FileChangeItem) -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit,
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
                text = "Changes",
                style = MaterialTheme.typography.titleLarge,
                color = ApColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${changes.size} file${if (changes.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                changes.forEach { change ->
                    val selectedRow = selected?.path == change.path
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedRow) ApColors.AccentSoft else ApColors.SurfaceAlt,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelect(change) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = change.path.substringAfterLast('\\').substringAfterLast('/'),
                            color = ApColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        DiffStat(added = change.addedLines, removed = change.removedLines)
                    }
                }
            }

            HorizontalDivider(
                color = ApColors.Border,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            if (selected != null) {
                Text(
                    text = selected.path,
                    color = ApColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                DiffStat(
                    added = selected.addedLines,
                    removed = selected.removedLines,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
                UnifiedDiffView(
                    diff = selected.unifiedDiff.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { onReject(selected.path) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Restore", color = ApColors.Danger)
                    }
                    Button(
                        onClick = { onAccept(selected.path) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApColors.Accent,
                            contentColor = ApColors.Background,
                        ),
                    ) {
                        Text("Keep", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(
                    text = "Select a file to preview the diff",
                    color = ApColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun DiffStat(
    added: Int,
    removed: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (added > 0) {
            Text(
                text = "+$added",
                color = ApColors.Success,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (removed > 0) {
            Text(
                text = "-$removed",
                color = ApColors.Danger,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun UnifiedDiffView(
    diff: String,
    modifier: Modifier = Modifier,
) {
    val lines = remember(diff) { diff.lines().ifEmpty { listOf("(no diff)") } }
    Column(
        modifier = modifier
            .background(ApColors.Background, RoundedCornerShape(12.dp))
            .border(1.dp, ApColors.Border, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        lines.forEach { line ->
            val color = when {
                line.startsWith("+++") || line.startsWith("---") -> ApColors.TextMuted
                line.startsWith('+') -> ApColors.Success
                line.startsWith('-') -> ApColors.Danger
                line.startsWith("@@") -> ApColors.Info
                else -> ApColors.TextPrimary
            }
            Text(
                text = line.ifEmpty { " " },
                color = color,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
            )
        }
    }
}

fun countDiffLines(unifiedDiff: String?): Pair<Int, Int> {
    if (unifiedDiff.isNullOrBlank()) return 0 to 0
    var added = 0
    var removed = 0
    unifiedDiff.lineSequence().forEach { line ->
        when {
            line.startsWith("+++") || line.startsWith("---") -> Unit
            line.startsWith('+') -> added++
            line.startsWith('-') -> removed++
        }
    }
    return added to removed
}
