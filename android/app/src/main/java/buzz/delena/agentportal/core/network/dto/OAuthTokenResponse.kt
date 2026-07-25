package buzz.delena.agentportal.core.network.dto

import kotlinx.serialization.Serializable

// Mirrors com.css.auth.dto.TokenResponse.java on the CSS auth server
// (css-next) verbatim: a Lombok @Getter @Builder POJO with no Jackson
// @JsonProperty overrides, so its JSON keys are the bare camelCase field
// names below -- read directly off that class, not guessed, and NOT the
// snake_case (access_token, refresh_token, ...) shape AppAuth's own
// OIDC-standard TokenResponse parser expects, which is why this app parses
// the POST oauth/token response into this DTO itself instead of handing it
// to AppAuth's built-in response model.
@Serializable
data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long? = null,
    val username: String? = null,
    val clientId: String? = null,
    val roles: List<String>? = null,
)
