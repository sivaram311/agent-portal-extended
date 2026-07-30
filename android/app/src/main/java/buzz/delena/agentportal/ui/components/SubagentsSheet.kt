package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.activity.SubagentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubagentsSheet(
    subagents: List<SubagentItem>,
    showFinished: Boolean,
    isBusy: Boolean,
    onToggleFinished: () -> Unit,
    onAbandon: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val active = subagents.filter { it.active }
    val finished = subagents.filter { !it.active }
    val visible = if (showFinished) active + finished else active

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sub-agents",
                    style = MaterialTheme.typography.titleLarge,
                    color = ApColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (finished.isNotEmpty()) {
                    TextButton(onClick = onToggleFinished) {
                        Text(
                            text = if (showFinished) {
                                "Hide finished (${finished.size})"
                            } else {
                                "Show finished (${finished.size})"
                            },
                            color = ApColors.Accent,
                        )
                    }
                }
            }

            Text(
                text = when {
                    subagents.isEmpty() -> "No sub-agents for this session."
                    visible.isEmpty() ->
                        "No active sub-agents." +
                            if (finished.isNotEmpty()) " ${finished.size} finished — use Show finished." else ""
                    else -> "${active.size} active · ${finished.size} finished"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ApColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                visible.forEach { item ->
                    SubagentRow(
                        item = item,
                        isBusy = isBusy,
                        onAbandon = { onAbandon(item.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubagentRow(
    item: SubagentItem,
    isBusy: Boolean,
    onAbandon: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = ApColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            StatusPill(
                label = friendlyStatusLabel(item.status.orEmpty().ifBlank { "unknown" }),
                tone = statusToneFor(item.status.orEmpty()),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (item.active) {
            Button(
                onClick = onAbandon,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ApColors.Danger,
                    contentColor = ApColors.TextPrimary,
                ),
            ) {
                Text("Abandon")
            }
        }
    }
}
