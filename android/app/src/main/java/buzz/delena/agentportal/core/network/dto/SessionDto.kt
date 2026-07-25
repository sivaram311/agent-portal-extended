package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
enum class SessionStatus {
    IDLE,
    STREAMING,
    WAITING_PERMISSION,
    WAITING_PLAN,
    COMPLETED,
    FAILED,
    CANCELLED,
    ARCHIVED,
}

// Mirrors com.agentportal.dto.SessionDto (backend Java record). The record's
// id, platformTaskId, createdAt and updatedAt are UUID/Instant on the
// backend, but Jackson serializes all of those as plain JSON strings, so
// they are modeled here as String to avoid needing custom serializers.
@Serializable
data class SessionDto(
    val id: String,
    val title: String? = null,
    val workspacePath: String,
    val cursorSessionId: String? = null,
    val status: SessionStatus,
    val provider: String? = null,
    val ownerUsername: String? = null,
    val platformRole: String? = null,
    val platformTaskId: String? = null,
    val allowedTools: List<String> = emptyList(),
    val allowedActions: List<String> = emptyList(),
    val rolePromptHint: String? = null,
    val humanApprovalRequired: Boolean? = null,
    val createdAt: String,
    val updatedAt: String,
)
