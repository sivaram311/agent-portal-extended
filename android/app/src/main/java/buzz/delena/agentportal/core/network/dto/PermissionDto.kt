package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
enum class PermissionStatus {
    PENDING,
    ALLOW_ONCE,
    ALLOW_ALWAYS,
    REJECT_ONCE,
    EXPIRED,
}

// Mirrors com.agentportal.dto.PermissionDto. id/sessionId are UUID and
// createdAt is Instant on the backend; modeled as String for the same
// reasons noted in SessionDto.kt.
@Serializable
data class PermissionDto(
    val id: String,
    val sessionId: String,
    val toolCallId: String? = null,
    val detailsJson: String? = null,
    val status: PermissionStatus,
    val kind: String? = null,
    val planMarkdown: String? = null,
    val createdAt: String,
)
