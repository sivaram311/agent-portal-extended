package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.data.TokenStore
import buzz.delena.agentportal.core.network.dto.OAuthTokenResponse
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Matches com.css.auth.dto.RefreshTokenRequest.java field names exactly.
@Serializable
private data class RefreshTokenRequest(
    val refreshToken: String,
    val clientId: String,
)

/**
 * Synchronous access-token refresh, shared by the REST retry-on-403
 * interceptor (NetworkModule) and StompWebSocketClient's own reconnect
 * logic -- both need the exact same "hit POST {authUrl}/auth/refresh,
 * save the result" behavior, just triggered from different places.
 *
 * Uses a bare HttpURLConnection rather than routing back through
 * Retrofit/OkHttp, to avoid any risk of recursing into whichever client
 * the caller is itself intercepting.
 *
 * The CSS auth server's refresh response does not rotate the refresh
 * token (com.css.auth.service.AuthenticationService#refresh never sets
 * one on the returned TokenResponse) -- TokenStore.saveTokens already
 * treats a null refreshToken as "keep the existing one," so callers don't
 * need to special-case that.
 */
object TokenRefresher {

    private val lock = Any()

    /**
     * Returns true if a new access token was obtained and saved.
     *
     * [clearOnFailure] only clears storage when the auth server explicitly
     * rejects the refresh (4xx). Transient network / 5xx failures leave
     * tokens in place so a still-valid access JWT (e.g. ~14m left) is not
     * wiped by a blip — that was showing as "session expired" while the
     * status strip still said Token · 14m left.
     */
    fun tryRefresh(tokenStore: TokenStore, clearOnFailure: Boolean = true): Boolean {
        synchronized(lock) {
            val refreshToken = tokenStore.getRefreshToken() ?: return false
            val authUrl = tokenStore.getAuthUrl() ?: return false
            val refreshPath = tokenStore.getRefreshPath() ?: return false
            val clientId = tokenStore.getClientId() ?: return false

            val result = runCatching {
                refreshSync(authUrl.trimEnd('/') + refreshPath, refreshToken, clientId)
            }

            val newTokens = result.getOrNull()
        if (newTokens != null) {
            tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
            return true
        }

        val failure = result.exceptionOrNull()
        android.util.Log.w("TokenRefresher", "Token refresh failed", failure)
        buzz.delena.agentportal.core.diagnostics.DiagnosticLogBuffer.append(
            "W",
            "TokenRefresher",
            "Token refresh failed: ${failure?.message}",
            failure,
        )
            val rejectedByServer = failure is RefreshRejectedException
            // Automatic paths clear only on definitive auth rejection so a
            // flaky network does not kick the user out while JWT TTL still shows.
            if (clearOnFailure && rejectedByServer) {
                tokenStore.clear()
            }
            return false
        }
    }

    private fun refreshSync(refreshUrl: String, refreshToken: String, clientId: String): OAuthTokenResponse {
        val body = NetworkModule.json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(refreshToken = refreshToken, clientId = clientId),
        )
        val connection = (URL(refreshUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
            if (status in 400..499) {
                throw RefreshRejectedException("Token refresh rejected ($status): $responseBody")
            }
            check(status in 200..299) { "Token refresh failed ($status): $responseBody" }
            return NetworkModule.json.decodeFromString(OAuthTokenResponse.serializer(), responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private class RefreshRejectedException(message: String) : IllegalStateException(message)
}
