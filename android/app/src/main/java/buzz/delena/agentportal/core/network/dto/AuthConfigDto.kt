package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Mirrors the response body built by AuthConfigController.config() -- a
// LinkedHashMap on the backend, not a Java record, but the key set and
// nullability below were read directly off that method.
@Serializable
data class AuthConfigDto(
    val cssEnabled: Boolean,
    val authUrl: String? = null,
    val issuer: String? = null,
    val clientId: String? = null,
    val loginPath: String,
    val refreshPath: String,
    val authMode: String,
    val oauthRedirectPath: String,
    val apiKeyFallbackEnabled: Boolean,
    val openAccess: Boolean,
    val authRequired: Boolean,
)
