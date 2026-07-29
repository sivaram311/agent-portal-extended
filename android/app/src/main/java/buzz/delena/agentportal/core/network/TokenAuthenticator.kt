package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.data.AuthSessionInfo
import buzz.delena.agentportal.core.data.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries a request once after a transparent token refresh when the
 * response is 403.
 *
 * NOT an okhttp3.Authenticator, deliberately: OkHttp only invokes
 * Authenticator automatically on HTTP 401, but this backend's Spring
 * Security setup returns 403 for every unauthenticated/expired-token
 * request on the api and ws paths (confirmed directly against the live
 * server -- curl with no token, an invalid token, and an expired token
 * all return 403, never 401; there's no AuthenticationEntryPoint
 * configured to challenge with 401 + WWW-Authenticate). An
 * Authenticator-based first attempt at this fix was reviewed, shipped,
 * and never once fired, because the trigger condition it relied on
 * never occurs on this backend. This plain Interceptor sees every
 * response regardless of status code and retries manually instead.
 *
 * Safe to treat 403 as "try a token refresh" specifically on this API
 * surface: SecurityConfig applies only .authenticated() (no
 * .hasRole/.hasAuthority checks) to those paths, so a 403 here is
 * definitionally "not authenticated," never a legitimate authorization
 * or permission denial that a token refresh wouldn't fix.
 */
class TokenAuthenticator(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 403 || tokenStore.getRefreshToken() == null) {
            return response
        }

        response.close()
        // Only wipe storage when the access token is already expired/near-expiry
        // and the auth server rejects refresh. If the JWT still has minutes left,
        // a failed refresh is treated as transient (network) — do not clear.
        val info = AuthSessionInfo.from(tokenStore)
        val accessNearExpiry = info.accessTokenExpired ||
            (info.accessTokenExpiresInSeconds != null && info.accessTokenExpiresInSeconds <= 120L)
        val refreshed = TokenRefresher.tryRefresh(
            tokenStore,
            clearOnFailure = accessNearExpiry,
        )
        if (!refreshed) {
            // Refresh failed -- re-issue the original request as-is so the
            // caller still gets a real (403) response to handle/surface,
            // rather than silently swallowing it here.
            return chain.proceed(request)
        }

        val newToken = tokenStore.getAccessToken() ?: return chain.proceed(request)
        val retried = request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
        return chain.proceed(retried)
    }
}
