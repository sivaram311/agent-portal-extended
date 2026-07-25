package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

// Mirrors com.agentportal.dto.MessageDto. id/sessionId are UUID and
// createdAt is Instant on the backend; both serialize as plain JSON strings.
@Serializable
data class MessageDto(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val sequenceNo: Long,
    val createdAt: String,
)
