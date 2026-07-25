package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Mirrors com.agentportal.dto.PromptRequest. prompt is @NotBlank server-side.
@Serializable
data class PromptRequest(
    val prompt: String,
)
