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

    /** Returns true if a new access token was obtained and saved. */
    fun tryRefresh(tokenStore: TokenStore, clearOnFailure: Boolean = true): Boolean {
        val refreshToken = tokenStore.getRefreshToken() ?: return false
        val authUrl = tokenStore.getAuthUrl() ?: return false
        val refreshPath = tokenStore.getRefreshPath() ?: return false
        val clientId = tokenStore.getClientId() ?: return false

        val newTokens = runCatching {
            refreshSync(authUrl.trimEnd('/') + refreshPath, refreshToken, clientId)
        }.getOrNull()

        if (newTokens == null) {
            // Automatic paths (REST 403 interceptor / WS handshake) clear so
            // the next navigation lands on login. Manual "Reconnect" passes
            // clearOnFailure=false so a transient network blip doesn't wipe
            // the session — the UI can offer Sign out instead.
            if (clearOnFailure) {
                tokenStore.clear()
            }
            return false
        }

        tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
        return true
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
            check(status in 200..299) { "Token refresh failed ($status): $responseBody" }
            return NetworkModule.json.decodeFromString(OAuthTokenResponse.serializer(), responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
