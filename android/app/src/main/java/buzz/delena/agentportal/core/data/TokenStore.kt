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
        val editor = prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "agent_portal_token_store"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
