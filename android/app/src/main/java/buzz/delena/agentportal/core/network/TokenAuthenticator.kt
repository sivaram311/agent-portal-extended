package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.core.data.TokenStore
import buzz.delena.agentportal.core.network.dto.OAuthTokenResponse
import kotlinx.serialization.Serializable
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
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
 * Real cause of "the app silently stops working after ~15 minutes": access
 * tokens expire (expiresIn=900s) and, without this, every subsequent
 * authenticated call just failed with 401 forever -- no refresh, no error
 * shown anywhere (see ChatViewModel.sendPrompt, which used to discard its
 * Result entirely). This is an OkHttp Authenticator: on a 401, refresh once
 * synchronously and retry the original request with the new token.
 *
 * Runs on a background thread by OkHttp's own contract for Authenticator
 * (blocking here is expected/correct, not a bug). Uses a bare
 * HttpURLConnection rather than routing back through Retrofit/the same
 * OkHttpClient this Authenticator is attached to, to avoid any risk of
 * recursing into itself.
 *
 * The CSS auth server's refresh response does not rotate the refresh token
 * (com.css.auth.service.AuthenticationService#refresh never sets one on the
 * returned TokenResponse) -- TokenStore.saveTokens already treats a null
 * refreshToken as "keep the existing one," so that's handled correctly
 * without special-casing here.
 */
class TokenAuthenticator(private val tokenStore: TokenStore) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once for this request chain -- give up rather than
        // loop forever if the refreshed token also comes back 401 (e.g. the
        // refresh token itself is expired/revoked).
        if (responseCount(response) >= 2) {
            return null
        }

        val refreshToken = tokenStore.getRefreshToken() ?: return null
        val authUrl = tokenStore.getAuthUrl() ?: return null
        val refreshPath = tokenStore.getRefreshPath() ?: return null
        val clientId = tokenStore.getClientId() ?: return null

        val newTokens = runCatching {
            refreshSync(authUrl.trimEnd('/') + refreshPath, refreshToken, clientId)
        }.getOrNull()

        if (newTokens == null) {
            // Refresh token itself is invalid/expired -- no way to recover
            // without a fresh login. Clearing tokens means the next screen
            // read of AuthRepository.isLoggedIn() correctly routes back to
            // the login screen instead of looping on 401s forever.
            tokenStore.clear()
            return null
        }

        tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
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
