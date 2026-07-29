package buzz.delena.agentportal.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed storage for the access/refresh tokens returned by the CSS
 * auth server. Agent Portal sessions can run shell commands and edit files
 * on the user's behalf, so a leaked plaintext token is real blast radius --
 * this deliberately uses EncryptedSharedPreferences, not plain
 * SharedPreferences or DataStore.
 */
class TokenStore(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasAccessToken(): Boolean = !getAccessToken().isNullOrBlank()

    fun saveTokens(accessToken: String, refreshToken: String? = null) {
        // commit() so TokenAuthenticator / WS refresh see the new token
        // immediately (apply() is async and raced with the next 403 retry).
        val editor = prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.commit()
    }

    fun saveAuthMethod(method: AuthMethod) {
        prefs.edit().putString(KEY_AUTH_METHOD, method.name).commit()
    }

    fun getAuthMethod(): AuthMethod {
        val raw = prefs.getString(KEY_AUTH_METHOD, null) ?: return AuthMethod.UNKNOWN
        return runCatching { AuthMethod.valueOf(raw) }.getOrDefault(AuthMethod.UNKNOWN)
    }

    // Cached alongside the tokens so a background token-refresh (which has no
    // Activity/ViewModel context to re-fetch /api/auth/config from) knows
    // where to POST the refresh request. Saved every time getAuthConfig()
    // succeeds (see AuthRepository), not just at login, so it stays current.
    fun saveAuthServer(authUrl: String, refreshPath: String, clientId: String) {
        prefs.edit()
            .putString(KEY_AUTH_URL, authUrl)
            .putString(KEY_REFRESH_PATH, refreshPath)
            .putString(KEY_CLIENT_ID, clientId)
            .commit()
    }

    fun getAuthUrl(): String? = prefs.getString(KEY_AUTH_URL, null)

    fun getRefreshPath(): String? = prefs.getString(KEY_REFRESH_PATH, null)

    fun getClientId(): String? = prefs.getString(KEY_CLIENT_ID, null)

    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val PREFS_FILE_NAME = "agent_portal_token_store"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_AUTH_URL = "auth_url"
        const val KEY_REFRESH_PATH = "refresh_path"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_AUTH_METHOD = "auth_method"
    }
}
