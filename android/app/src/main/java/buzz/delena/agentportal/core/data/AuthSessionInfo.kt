package buzz.delena.agentportal.core.data

import android.util.Base64
import buzz.delena.agentportal.core.network.NetworkModule
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Snapshot of the signed-in session for the connection-status strip.
 * JWT claims are decoded locally for display only (not signature-verified —
 * the portal/CSS already enforced that on issue).
 */
data class AuthSessionInfo(
    val authMethod: AuthMethod,
    val subject: String?,
    val clientId: String?,
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val accessTokenExpired: Boolean,
    val accessTokenExpiresInSeconds: Long?,
    val authServerHost: String?,
    val claimsDecodable: Boolean,
) {
    val tokenStateLabel: String
        get() = when {
            !hasAccessToken -> "No access token"
            !claimsDecodable -> "Access token present"
            accessTokenExpired -> "Access token expired"
            accessTokenExpiresInSeconds == null -> "Access token present"
            accessTokenExpiresInSeconds < 60L -> "Token · ${accessTokenExpiresInSeconds}s left"
            accessTokenExpiresInSeconds < 3600L ->
                "Token · ${accessTokenExpiresInSeconds / 60}m left"
            else -> "Token · ${accessTokenExpiresInSeconds / 3600}h left"
        }

    val refreshStateLabel: String
        get() = when {
            hasRefreshToken -> "Refresh ready"
            accessTokenExpired -> "Refresh missing — sign in again"
            else -> "No refresh token"
        }

    /** Red only when signed out or unrecoverable without a new login. */
    val needsSignIn: Boolean
        get() = !hasAccessToken || (accessTokenExpired && !hasRefreshToken)

    /** Amber: access expired but refresh can recover — show Reconnect. */
    val needsReconnect: Boolean
        get() = hasAccessToken && accessTokenExpired && hasRefreshToken

    val canReconnect: Boolean
        get() = hasRefreshToken

    companion object {
        fun from(tokenStore: TokenStore): AuthSessionInfo {
            val access = tokenStore.getAccessToken()
            val claims = access?.let { decodeJwtClaims(it) }
            val expEpochSec = claims?.let { readExpSeconds(it["exp"]) }
            val nowSec = System.currentTimeMillis() / 1000L
            val expiresIn = expEpochSec?.let { it - nowSec }
            val authUrl = tokenStore.getAuthUrl()
            return AuthSessionInfo(
                authMethod = tokenStore.getAuthMethod(),
                subject = claims?.get("sub")?.jsonPrimitive?.contentOrNull,
                clientId = claims?.get("client_id")?.jsonPrimitive?.contentOrNull
                    ?: tokenStore.getClientId(),
                hasAccessToken = !access.isNullOrBlank(),
                hasRefreshToken = !tokenStore.getRefreshToken().isNullOrBlank(),
                accessTokenExpired = expiresIn != null && expiresIn <= 0L,
                accessTokenExpiresInSeconds = expiresIn,
                authServerHost = authUrl?.let { hostOf(it) },
                claimsDecodable = access.isNullOrBlank() || claims != null,
            )
        }

        private fun readExpSeconds(element: JsonElement?): Long? {
            if (element == null) return null
            element.jsonPrimitive.longOrNull?.let { return it }
            return element.jsonPrimitive.contentOrNull?.toLongOrNull()
        }

        private fun decodeJwtClaims(token: String) = runCatching {
            val parts = token.split('.')
            if (parts.size < 2) return@runCatching null
            val payload = parts[1]
            val pad = (4 - payload.length % 4) % 4
            val padded = payload + "=".repeat(pad)
            val decoded = Base64.decode(
                padded,
                Base64.URL_SAFE or Base64.NO_WRAP,
            )
            NetworkModule.json.parseToJsonElement(String(decoded, Charsets.UTF_8)).jsonObject
        }.getOrNull()

        private fun hostOf(url: String): String = runCatching {
            val withoutScheme = url.substringAfter("://", url)
            withoutScheme.substringBefore('/').substringBefore('?')
        }.getOrDefault(url)
    }
}
