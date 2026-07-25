package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Request body posted straight to the CSS auth server at
// "{authConfig.authUrl}{authConfig.loginPath}" -- this is not one of this
// app's own backend endpoints.
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val clientId: String? = null,
)

// The CSS auth server's login response is not part of this app's own
// backend source, so only accessToken is confirmed (verified against
// workspaces/agent-api/PREPROD-curl-recipe.sh, which greps accessToken out
// of the login response body). refreshToken/tokenType/expiresIn are
// plausible given the auth config's refreshPath but unverified against the
// CSS server source, so they stay nullable with defaults; the shared Json
// instance also ignores unrecognized keys as a second layer of tolerance.
@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long? = null,
)
