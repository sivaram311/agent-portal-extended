package buzz.delena.agentportal.ui.activity

import buzz.delena.agentportal.core.network.dto.ToolRunDto
import buzz.delena.agentportal.ui.screens.ChatMessageItem
import buzz.delena.agentportal.ui.screens.ToolCategory
import buzz.delena.agentportal.ui.screens.ToolStepItem
import java.time.Instant

/**
 * Turn-scoped tool activity: filter noise, categorize, and summarize for Claude-style chips.
 */
object ToolActivity {

    private val READ_HINTS = listOf(
        "read", "grep", "glob", "search", "list", "ls", "cat", "find", "rg", "stat", "file",
    )
    private val EDIT_HINTS = listOf(
        "write", "edit", "apply", "strreplace", "search_replace", "delete", "create", "patch", "update",
    )
    private val SHELL_HINTS = listOf(
        "shell", "bash", "terminal", "cmd", "powershell", "pwsh", "run_terminal", "execute",
    )

    fun buildTurnActivity(
        allTools: List<ToolRunDto>,
        messages: List<ChatMessageItem>,
        showReads: Boolean,
        maxSteps: Int = 40,
    ): TurnActivityModel {
        val turnStart = lastUserMessageInstant(messages)
        val turnScoped = if (turnStart != null) {
            allTools.filter { tool ->
                val started = parseInstant(tool.startedAt) ?: parseInstant(tool.finishedAt)
                started == null || !started.isBefore(turnStart)
            }
        } else {
            allTools
        }

        val cleaned = dedupe(turnScoped)
            .filter { isMeaningful(it) }
            .map { it.toStep() }

        val reads = cleaned.filter { it.category == ToolCategory.READ }
        val edits = cleaned.filter { it.category == ToolCategory.EDIT }
        val shells = cleaned.filter { it.category == ToolCategory.SHELL }
        val other = cleaned.filter { it.category == ToolCategory.OTHER }
        val failed = cleaned.filter {
            it.status.equals("failed", true) || it.status.equals("error", true)
        }
        val running = cleaned.filter {
            it.status.equals("running", true) || it.status.equals("in_progress", true)
        }

        val primary = (edits + shells + other + failed).distinctBy { it.id }
        val visible = if (showReads) cleaned else primary
        val capped = visible.takeLast(maxSteps)

        val chipLabels = buildChipLabels(
            turnScoped = turnStart != null,
            edits = edits.size,
            shells = shells.size,
            other = other.size,
            reads = reads.size,
            failed = failed.size,
            running = running.size,
            sessionTotal = allTools.size,
            turnTotal = cleaned.size,
        )

        return TurnActivityModel(
            steps = capped,
            reads = reads.takeLast(maxSteps),
            edits = edits,
            shells = shells,
            other = other,
            chipLabels = chipLabels,
            turnScoped = turnStart != null,
            showReads = showReads,
            readCount = reads.size,
            attentionCount = primary.size,
            failedCount = failed.size,
            runningCount = running.size,
            sessionRawCount = allTools.size,
        )
    }

    private fun buildChipLabels(
        turnScoped: Boolean,
        edits: Int,
        shells: Int,
        other: Int,
        reads: Int,
        failed: Int,
        running: Int,
        sessionTotal: Int,
        turnTotal: Int,
    ): List<ActivityChipLabel> {
        val chips = mutableListOf<ActivityChipLabel>()
        val attention = edits + shells + other
        val prefix = if (turnScoped) null else "Session"

        when {
            running > 0 && attention + reads > 0 -> {
                chips += ActivityChipLabel(
                    text = buildString {
                        if (prefix != null) append("$prefix · ")
                        append("Running $running")
                        if (attention > 0) append(" · ${attentionLabel(edits, shells, other)}")
                        if (failed > 0) append(" · $failed failed")
                    },
                    kind = ChipKind.PRIMARY,
                )
            }
            attention > 0 -> {
                chips += ActivityChipLabel(
                    text = buildString {
                        if (prefix != null) append("$prefix · ")
                        append(if (shells > 0 && edits == 0 && other == 0) {
                            "Ran $shells command${s(shells)}"
                        } else {
                            "Ran ${attentionLabel(edits, shells, other)}"
                        })
                        if (failed > 0) append(" · $failed failed")
                    },
                    kind = ChipKind.PRIMARY,
                )
            }
            reads > 0 -> {
                chips += ActivityChipLabel(
                    text = buildString {
                        if (prefix != null) append("$prefix · ")
                        append("Read $reads file${s(reads)}")
                    },
                    kind = ChipKind.READS,
                )
            }
            sessionTotal > 0 && turnTotal == 0 -> {
                chips += ActivityChipLabel(
                    text = "Session activity · $sessionTotal steps (filtered)",
                    kind = ChipKind.PRIMARY,
                )
            }
        }

        if (reads > 0 && attention > 0) {
            chips += ActivityChipLabel(
                text = "Read $reads file${s(reads)}",
                kind = ChipKind.READS,
            )
        }
        return chips
    }

    private fun attentionLabel(edits: Int, shells: Int, other: Int): String {
        val parts = mutableListOf<String>()
        if (edits > 0) parts += "$edits edit${s(edits)}"
        if (shells > 0) parts += "$shells command${s(shells)}"
        if (other > 0) parts += "$other other"
        return if (parts.isEmpty()) "0 tools" else parts.joinToString(" · ")
    }

    private fun s(n: Int) = if (n == 1) "" else "s"

    private fun isMeaningful(tool: ToolRunDto): Boolean {
        val name = tool.toolName?.trim().orEmpty()
        val kind = tool.kind?.trim().orEmpty()
        val callId = tool.toolCallId?.trim().orEmpty()
        if (kind.equals("subagent", ignoreCase = true)) return false
        if (name.isEmpty() || name.equals("tool", ignoreCase = true)) return false
        if (name.matches(Regex("""task-.*\.log""", RegexOption.IGNORE_CASE))) return false
        if (callId.matches(Regex("""task-.*\.log""", RegexOption.IGNORE_CASE))) return false
        if (name.contains("subagent", ignoreCase = true) && !EDIT_HINTS.any { name.contains(it, true) }) {
            // Subagent-ish titles without edit semantics — keep out of primary tools list.
            if (name.contains("agent", ignoreCase = true) || name.contains("task", ignoreCase = true)) {
                return false
            }
        }
        if (tool.status.equals("abandoned", ignoreCase = true)) return false
        return true
    }

    private fun dedupe(tools: List<ToolRunDto>): List<ToolRunDto> {
        val byCall = LinkedHashMap<String, ToolRunDto>()
        for (tool in tools) {
            val key = tool.toolCallId?.takeIf { it.isNotBlank() } ?: tool.id
            val existing = byCall[key]
            if (existing == null) {
                byCall[key] = tool
            } else {
                // Keep the later / more complete status.
                byCall[key] = preferRicher(existing, tool)
            }
        }
        return byCall.values.toList()
    }

    private fun preferRicher(a: ToolRunDto, b: ToolRunDto): ToolRunDto {
        val aDone = a.status.equals("completed", true) || a.status.equals("failed", true)
        val bDone = b.status.equals("completed", true) || b.status.equals("failed", true)
        return when {
            bDone && !aDone -> b
            aDone && !bDone -> a
            (b.output?.length ?: 0) > (a.output?.length ?: 0) -> b
            else -> b
        }
    }

    fun categorize(name: String?, kind: String?): ToolCategory {
        val n = (name ?: "").lowercase()
        val k = (kind ?: "").lowercase()
        val hay = "$n $k"
        return when {
            READ_HINTS.any { hay.contains(it) } -> ToolCategory.READ
            EDIT_HINTS.any { hay.contains(it) } -> ToolCategory.EDIT
            SHELL_HINTS.any { hay.contains(it) } -> ToolCategory.SHELL
            else -> ToolCategory.OTHER
        }
    }

    private fun ToolRunDto.toStep() = ToolStepItem(
        id = id,
        title = toolName?.ifBlank { null } ?: kind ?: "Tool",
        status = status,
        subtitle = argsJson?.take(160),
        output = output,
        kind = kind,
        category = categorize(toolName, kind),
        startedAt = startedAt,
        toolCallId = toolCallId,
    )

    /** Same filter as the web Sub-agents panel (kind=subagent or agent/task name). */
    fun extractSubagents(allTools: List<ToolRunDto>): List<SubagentItem> {
        return dedupe(allTools)
            .filter { isSubagentRow(it) }
            .map { tool ->
                val key = tool.subagentId?.takeIf { it.isNotBlank() }
                    ?: tool.toolCallId?.takeIf { it.isNotBlank() }
                    ?: tool.id
                SubagentItem(
                    key = key,
                    title = tool.toolName?.ifBlank { null } ?: "Sub-agent",
                    status = tool.status,
                    active = isActiveStatus(tool.status),
                )
            }
            .distinctBy { it.key }
    }

    private fun isSubagentRow(tool: ToolRunDto): Boolean {
        val kind = tool.kind?.trim().orEmpty()
        if (kind.equals("subagent", ignoreCase = true)) return true
        val name = tool.toolName?.trim().orEmpty()
        return name.contains("agent", ignoreCase = true) || name.contains("task", ignoreCase = true)
    }

    private fun isActiveStatus(status: String?): Boolean {
        val s = status?.trim()?.lowercase().orEmpty()
        return s == "running" || s == "pending" || s == "in_progress" || s == "in-progress"
    }

    private fun lastUserMessageInstant(messages: List<ChatMessageItem>): Instant? {
        val lastUser = messages.lastOrNull { it.isUser } ?: return null
        return parseInstant(lastUser.timeLabel)
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching {
                // Tolerate offsets without colon, or space separator.
                Instant.parse(raw.replace(' ', 'T'))
            }.getOrNull()
    }
}

data class TurnActivityModel(
    val steps: List<ToolStepItem>,
    val reads: List<ToolStepItem>,
    val edits: List<ToolStepItem>,
    val shells: List<ToolStepItem>,
    val other: List<ToolStepItem>,
    val chipLabels: List<ActivityChipLabel>,
    val turnScoped: Boolean,
    val showReads: Boolean,
    val readCount: Int,
    val attentionCount: Int,
    val failedCount: Int,
    val runningCount: Int,
    val sessionRawCount: Int,
)

data class ActivityChipLabel(
    val text: String,
    val kind: ChipKind,
)

data class SubagentItem(
    val key: String,
    val title: String,
    val status: String?,
    val active: Boolean,
)

enum class ChipKind {
    PRIMARY,
    READS,
    SUBAGENTS,
}
