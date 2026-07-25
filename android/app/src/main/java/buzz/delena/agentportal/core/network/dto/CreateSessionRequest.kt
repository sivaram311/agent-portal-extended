package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Mirrors com.agentportal.dto.CreateSessionRequest. workspacePath is
// @NotBlank server-side; platformTaskId is UUID on the backend but modeled
// as String here for the same reason as SessionDto.id.
@Serializable
data class CreateSessionRequest(
    val title: String? = null,
    val workspacePath: String,
    val provider: String? = null,
    val useGuidanceDefaults: Boolean? = null,
    val platformRole: String? = null,
    val platformTaskId: String? = null,
)
