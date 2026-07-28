package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToolRunDto(
    val id: String,
    val sessionId: String,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val argsJson: String? = null,
    val status: String? = null,
    val kind: String? = null,
    val parentToolCallId: String? = null,
    val subagentId: String? = null,
    val output: String? = null,
    val exitCode: Int? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null,
)

@Serializable
data class FileChangeDto(
    val path: String,
    val status: String? = null,
    val size: Long = 0,
    val unifiedDiff: String? = null,
    val source: String? = null,
)

@Serializable
data class PathRequest(
    val path: String,
)
