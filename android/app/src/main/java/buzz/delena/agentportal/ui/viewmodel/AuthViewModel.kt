package buzz.delena.agentportal.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.AuthRepository
import buzz.delena.agentportal.core.network.NetworkModule
import buzz.delena.agentportal.core.network.dto.OAuthTokenResponse
import buzz.delena.agentportal.ui.screens.LoginUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Must match the buzz.delena.agentportal://oauth/callback intent-filter already
// declared on MainActivity in AndroidManifest.xml -- not redeclared here, just
// referenced, since this app's own manifest and its appAuthRedirectScheme
// manifestPlaceholder already own that literal string.
private const val OAUTH_REDIRECT_URI = "buzz.delena.agentportal://oauth/callback"

// Body of POST {issuer}/oauth/token, matching css-next's
// com.css.auth.dto.TokenExchangeRequest.java field names exactly (read
// directly off that class, not guessed): grant_type, code, redirect_uri,
// client_id, code_verifier.
@Serializable
private data class OAuthTokenExchangeRequest(
    @SerialName("grant_type") val grantType: String = "authorization_code",
    val code: String,
    @SerialName("redirect_uri") val redirectUri: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("code_verifier") val codeVerifier: String,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // Cached between startSsoLogin's launch and completeSsoLogin's redirect
    // handling so the token exchange can supply the matching PKCE
    // code_verifier and validate the returned state. AuthorizationService is
    // kept until the Custom Tab finishes (disposing immediately cancels the
    // browser warm-up and can make SSO appear to do nothing).
    private var pendingAuthRequest: AuthorizationRequest? = null
    private var authorizationService: AuthorizationService? = null

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank() || current.isLoading) return

        _state.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.loginWithPassword(current.username, current.password)
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
                onSuccess()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Sign-in failed",
                )
            }
        }
    }

    /**
     * Fetches runtime auth config, builds an AppAuth PKCE AuthorizationRequest
     * against css-next's issuer (authConfig.issuer + oauth/authorize|token),
     * and hands the launch Intent back via [onReady] so the caller
     * (LoginScreen) can drive it through an ActivityResultLauncher /
     * Custom Tab. The request -- and its auto-generated PKCE code_verifier
     * and state -- is cached so a later call to [completeSsoLogin] can
     * finish the exchange.
     */
    fun startSsoLogin(context: Context, onReady: (Intent) -> Unit) {
        if (_state.value.isLoading) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val authConfig = authRepository.getAuthConfig().getOrThrow()
                val issuer = authConfig.issuer?.trimEnd('/')
                    ?: throw IllegalStateException("Auth server issuer is not configured")
                val clientId = authConfig.clientId
                    ?: throw IllegalStateException("OAuth client id is not configured")

                val serviceConfig = AuthorizationServiceConfiguration(
                    Uri.parse("$issuer/oauth/authorize"),
                    Uri.parse("$issuer/oauth/token"),
                )
                val request = AuthorizationRequest.Builder(
                    serviceConfig,
                    clientId,
                    ResponseTypeValues.CODE,
                    Uri.parse(OAUTH_REDIRECT_URI),
                )
                    .setScopes("openid")
                    .build()
                pendingAuthRequest = request

                authorizationService?.dispose()
                val authService = AuthorizationService(context)
                authorizationService = authService
                val intent = try {
                    authService.getAuthorizationRequestIntent(request)
                } catch (t: Throwable) {
                    authorizationService?.dispose()
                    authorizationService = null
                    throw IllegalStateException(
                        "No browser available for SSO. Install Chrome (or any browser) and try again.",
                        t,
                    )
                }

                _state.value = _state.value.copy(isLoading = false)
                withContext(Dispatchers.Main.immediate) {
                    onReady(intent)
                }
            } catch (t: Throwable) {
                authorizationService?.dispose()
                authorizationService = null
                pendingAuthRequest = null
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = t.message ?: "Could not start SSO sign-in",
                )
            }
        }
    }

    override fun onCleared() {
        authorizationService?.dispose()
        authorizationService = null
        super.onCleared()
    }

    /**
     * Called with the Intent delivered back to the app once css-next
     * completes the browser login and redirects to
     * buzz.delena.agentportal://oauth/callback. This app launches the
     * authorization request via AuthorizationService.getAuthorizationRequestIntent
     * (not performAuthorizationRequest), so the redirect always lands as a
     * plain VIEW intent whose data is the raw callback URI (see MainActivity's
     * manifest intent-filter) -- never the extras-wrapped intent AppAuth's own
     * AuthorizationManagementActivity would produce for the PendingIntent-based
     * flow. AuthorizationResponse.fromIntent / AuthorizationException.fromIntent
     * therefore do not apply here; this reads the redirect URI's own
     * code / state / error query params directly instead. Validates state,
     * exchanges the code directly against css-next's POST oauth/token (JSON
     * body, css-next's own response shape -- not AppAuth's OIDC-standard
     * TokenResponse parser, whose snake_case keys do not match css-next's
     * TokenResponse.java), and persists the result via
     * [AuthRepository.completeSsoLogin].
     */
    fun completeSsoLogin(intent: Intent, onSuccess: () -> Unit) {
        val request = pendingAuthRequest
        val redirectUri = intent.data
        if (request == null || redirectUri == null) {
            pendingAuthRequest = null
            disposeAuthService()
            _state.value = _state.value.copy(isLoading = false, error = "SSO sign-in was cancelled or incomplete")
            return
        }

        val oauthError = redirectUri.getQueryParameter("error")
        if (oauthError != null) {
            pendingAuthRequest = null
            disposeAuthService()
            val description = redirectUri.getQueryParameter("error_description")
            _state.value = _state.value.copy(isLoading = false, error = description ?: oauthError)
            return
        }

        val code = redirectUri.getQueryParameter("code")
        val returnedState = redirectUri.getQueryParameter("state")
        if (code.isNullOrBlank()) {
            pendingAuthRequest = null
            disposeAuthService()
            _state.value = _state.value.copy(isLoading = false, error = "SSO sign-in did not return an authorization code")
            return
        }
        if (returnedState != request.state) {
            pendingAuthRequest = null
            disposeAuthService()
            _state.value = _state.value.copy(isLoading = false, error = "SSO sign-in failed (state mismatch)")
            return
        }
        val codeVerifier = request.codeVerifier
        if (codeVerifier == null) {
            pendingAuthRequest = null
            disposeAuthService()
            _state.value = _state.value.copy(isLoading = false, error = "SSO sign-in failed (missing PKCE verifier)")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val authConfig = authRepository.getAuthConfig().getOrThrow()
                val issuer = authConfig.issuer?.trimEnd('/')
                    ?: throw IllegalStateException("Auth server issuer is not configured")

                val tokenResponse = exchangeCodeForTokens(
                    tokenEndpoint = "$issuer/oauth/token",
                    code = code,
                    redirectUri = OAUTH_REDIRECT_URI,
                    clientId = request.clientId,
                    codeVerifier = codeVerifier,
                )
                pendingAuthRequest = null
                disposeAuthService()

                authRepository.completeSsoLogin(tokenResponse).onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                }.onFailure { error ->
                    _state.value = _state.value.copy(isLoading = false, error = error.message ?: "Sign-in failed")
                }
            } catch (t: Throwable) {
                pendingAuthRequest = null
                disposeAuthService()
                _state.value = _state.value.copy(isLoading = false, error = t.message ?: "SSO token exchange failed")
            }
        }
    }

    private fun disposeAuthService() {
        authorizationService?.dispose()
        authorizationService = null
    }

    // Deliberately not routed through AuthApi/AgentPortalApi or AppAuth's
    // own performTokenRequest: this app talks to css-next's oauth/token
    // directly (not via this app's own backend BFF, which exists only to
    // work around browser CORS for the web frontend), and AppAuth's built-in
    // TokenResponse parser expects standard snake_case OAuth keys that don't
    // match css-next's TokenResponse.java (camelCase, no Jackson renaming).
    private suspend fun exchangeCodeForTokens(
        tokenEndpoint: String,
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String,
    ): OAuthTokenResponse = withContext(Dispatchers.IO) {
        val requestBody = NetworkModule.json.encodeToString(
            OAuthTokenExchangeRequest.serializer(),
            OAuthTokenExchangeRequest(
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier,
            ),
        )

        val connection = (URL(tokenEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(requestBody) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Token exchange failed ($status): $body")
            }
            NetworkModule.json.decodeFromString(OAuthTokenResponse.serializer(), body)
        } finally {
            connection.disconnect()
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
