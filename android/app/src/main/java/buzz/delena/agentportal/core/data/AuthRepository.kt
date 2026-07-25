package buzz.delena.agentportal.core.data

import buzz.delena.agentportal.core.network.AgentPortalApi
import buzz.delena.agentportal.core.network.AuthApi
import buzz.delena.agentportal.core.network.dto.LoginRequest

/**
 * Wraps the two-step login flow: fetch runtime auth config from this app's
 * own backend (GET api/auth/config), then POST credentials straight to the
 * CSS auth server at authConfig.authUrl + authConfig.loginPath. Login is
 * deliberately not one of this app's own backend endpoints.
 */
class AuthRepository(
    private val agentPortalApi: AgentPortalApi,
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {

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
