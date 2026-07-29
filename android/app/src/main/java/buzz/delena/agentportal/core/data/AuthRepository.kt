package buzz.delena.agentportal.core.data

import buzz.delena.agentportal.core.network.AgentPortalApi
import buzz.delena.agentportal.core.network.AuthApi
import buzz.delena.agentportal.core.network.TokenRefresher
import buzz.delena.agentportal.core.network.dto.AuthConfigDto
import buzz.delena.agentportal.core.network.dto.LoginRequest
import buzz.delena.agentportal.core.network.dto.OAuthTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val config = agentPortalApi.getAuthConfig()
            // Cached so TokenAuthenticator (no Activity/ViewModel context) knows
            // where to POST a token refresh later, without re-fetching this.
            if (config.authUrl != null && config.clientId != null) {
                tokenStore.saveAuthServer(config.authUrl, config.refreshPath, config.clientId)
            }
            Result.success(config)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun completeSsoLogin(tokenResponse: OAuthTokenResponse): Result<Unit> {
        return try {
            tokenStore.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
            tokenStore.saveAuthMethod(AuthMethod.SSO)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun loginWithPassword(username: String, password: String): Result<Unit> {
        return try {
            // getAuthConfig() (not the raw api call) so the auth-server config
            // gets cached for TokenAuthenticator the same way SSO login already does.
            val authConfig = getAuthConfig().getOrThrow()
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
            tokenStore.saveAuthMethod(AuthMethod.PASSWORD)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Manual reconnect: refresh auth-server cache, then refresh the access
     * token without wiping storage on failure (UI offers Sign out instead).
     */
    suspend fun reconnectSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            getAuthConfig().getOrThrow()
            if (tokenStore.getRefreshToken().isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("No refresh token stored — sign out and sign in again"),
                )
            }
            val ok = TokenRefresher.tryRefresh(tokenStore, clearOnFailure = false)
            if (!ok) {
                return@withContext Result.failure(
                    IllegalStateException("Could not refresh the access token — sign out and sign in again"),
                )
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun isLoggedIn(): Boolean = tokenStore.hasAccessToken()

    fun authSessionInfo(): AuthSessionInfo = AuthSessionInfo.from(tokenStore)

    fun logout() {
        tokenStore.clear()
    }
}
