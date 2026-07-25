package buzz.delena.agentportal.core.data

import buzz.delena.agentportal.core.network.AgentPortalApi
import buzz.delena.agentportal.core.network.AuthApi
import buzz.delena.agentportal.core.network.dto.AuthConfigDto
import buzz.delena.agentportal.core.network.dto.LoginRequest
import buzz.delena.agentportal.core.network.dto.OAuthTokenResponse

/**
 * Wraps the two-step login flow: fetch runtime auth config from this app's
 * own backend (GET api/auth/config), then POST credentials straight to the
 * CSS auth server at authConfig.authUrl + authConfig.loginPath. Login is
 * deliberately not one of this app's own backend endpoints.
 *
 * The SSO/OAuth-PKCE lane (completeSsoLogin below) is a second entry point
 * into the same token storage: the authorization-code exchange itself is
 * driven from the ViewModel/UI layer (it needs an Activity for AppAuth's
 * AuthorizationService and an ActivityResultLauncher, neither of which
 * belong in a repository), so this repository's job there is just
 * persisting whatever tokens that exchange produced.
 */
class AuthRepository(
    private val agentPortalApi: AgentPortalApi,
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {

    suspend fun getAuthConfig(): Result<AuthConfigDto> {
        return try {
            Result.success(agentPortalApi.getAuthConfig())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun completeSsoLogin(tokenResponse: OAuthTokenResponse): Result<Unit> {
        return try {
            tokenStore.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun loginWithPassword(username: String, password: String): Result<Unit> {
        return try {
            val authConfig = agentPortalApi.getAuthConfig()
            val authUrl = authConfig.authUrl
                ?: return Result.failure(IllegalStateException("Auth server URL is not configured"))
            val loginUrl = authUrl.trimEnd('/') + authConfig.loginPath

            val response = authApi.login(
                url = loginUrl,
                request = LoginRequest(
                    username = username,
                    password = password,
                    clientId = authConfig.clientId,
                ),
            )
            tokenStore.saveTokens(response.accessToken, response.refreshToken)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.hasAccessToken()

    fun logout() {
        tokenStore.clear()
    }
}
