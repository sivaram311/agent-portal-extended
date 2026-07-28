package buzz.delena.agentportal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

/**
 * Thumb-zone composer: mode chip + attach/mic affordances + send.
 * Mode/attach/mic are present for Claude-mobile parity; attach/mic are no-ops until wired.
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    modeLabel: String = "Auto",
    onModeClick: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ApColors.Surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = onModeClick,
                    label = { Text(modeLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ApColors.AccentSoft,
                        selectedLabelColor = ApColors.Accent,
                    ),
                )
                IconButton(onClick = onAttachClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach",
                        tint = ApColors.TextMuted,
                    )
                }
                IconButton(onClick = onVoiceClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice",
                        tint = ApColors.TextMuted,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 140.dp),
                    placeholder = { Text("Message the agent…") },
                    enabled = !isSending,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ApColors.SurfaceAlt,
                        unfocusedContainerColor = ApColors.SurfaceAlt,
                        disabledContainerColor = ApColors.SurfaceAlt,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = ApColors.Accent,
                        focusedTextColor = ApColors.TextPrimary,
                        unfocusedTextColor = ApColors.TextPrimary,
                    ),
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = !isSending && value.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ApColors.Accent,
                        contentColor = ApColors.Background,
                        disabledContainerColor = ApColors.SurfaceAlt,
                        disabledContentColor = ApColors.TextMuted,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ApColors.Background,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E293B)
@Composable
private fun ChatInputBarPreview() {
    AgentPortalTheme {
        ChatInputBar(
            value = "Run the test suite",
            onValueChange = {},
            onSend = {},
            isSending = false,
        )
    }
}
