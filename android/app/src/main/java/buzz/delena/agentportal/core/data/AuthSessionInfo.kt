package buzz.delena.agentportal.core.data

import android.util.Base64
import buzz.delena.agentportal.core.network.NetworkModule
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
) {
    val tokenStateLabel: String
        get() = when {
            !hasAccessToken -> "No access token"
            accessTokenExpired -> "Access token expired"
            accessTokenExpiresInSeconds == null -> "Access token present"
            accessTokenExpiresInSeconds <= 0L -> "Access token expired"
            accessTokenExpiresInSeconds < 60L -> "Token · ${accessTokenExpiresInSeconds}s left"
            accessTokenExpiresInSeconds < 3600L ->
                "Token · ${accessTokenExpiresInSeconds / 60}m left"
            else -> "Token · ${accessTokenExpiresInSeconds / 3600}h left"
        }

    val summaryLine: String
        get() = buildString {
            append(authMethod.label)
            if (!subject.isNullOrBlank()) {
                append(" · ")
                append(subject)
            }
            append(" · ")
            append(tokenStateLabel)
            if (hasRefreshToken) append(" · refresh OK") else append(" · no refresh")
            if (!authServerHost.isNullOrBlank()) {
                append(" · ")
                append(authServerHost)
            }
        }

    companion object {
        fun from(tokenStore: TokenStore): AuthSessionInfo {
            val access = tokenStore.getAccessToken()
            val claims = access?.let { decodeJwtClaims(it) }
            val expEpochSec = claims?.get("exp")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
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
            )
        }

        private fun decodeJwtClaims(token: String) = runCatching {
            val parts = token.split('.')
            if (parts.size < 2) return@runCatching null
            val decoded = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            NetworkModule.json.parseToJsonElement(String(decoded, Charsets.UTF_8)).jsonObject
        }.getOrNull()

        private fun hostOf(url: String): String = runCatching {
            val withoutScheme = url.substringAfter("://", url)
            withoutScheme.substringBefore('/').substringBefore('?')
        }.getOrDefault(url)
    }
}
