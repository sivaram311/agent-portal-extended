package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Mirrors com.agentportal.dto.PermissionDecisionRequest. decision is
// @NotBlank server-side and is treated as a free string matching one of the
// PermissionStatus entry names ("ALLOW_ONCE" / "ALLOW_ALWAYS" / "REJECT_ONCE").
@Serializable
data class PermissionDecisionRequest(
    val decision: String,
    val reason: String? = null,
)
