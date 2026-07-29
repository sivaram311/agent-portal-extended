package buzz.delena.agentportal.core.network

import buzz.delena.agentportal.BuildConfig
import buzz.delena.agentportal.core.data.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual composition root for the networking stack. There is no DI
 * framework in this module yet (no Hilt/Koin dependency is present), so
 * this is a plain object with factory functions the app wires up by hand;
 * swapping in DI later is a call-site change, not a rewrite of this file.
 */
object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val jsonMediaType = "application/json".toMediaType()

    // Paths (relative to API_BASE_URL, no leading slash) that must not
    // receive an Authorization header -- a token may not exist yet the
    // first time either of these is called.
    private val unauthenticatedPathPrefixes = listOf("api/auth/config", "api/health")

    private fun authInterceptor(tokenStore: TokenStore): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath.removePrefix("/")
        val skipAuth = unauthenticatedPathPrefixes.any { path.startsWith(it) }
        val token = tokenStore.getAccessToken()

        val request = if (!skipAuth && !token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * REST client for Agent Portal. Prompt/create can block on ACP handshake
     * for tens of seconds before any response bytes arrive — OkHttp's default
     * 10s read timeout aborted those calls (nginx 499) and left the session
     * stuck "already has an active run" for retries.
     */
    fun provideOkHttpClient(tokenStore: TokenStore): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.MINUTES)
            .addInterceptor(authInterceptor(tokenStore))
            .addInterceptor(TokenAuthenticator(tokenStore))
            .addInterceptor(loggingInterceptor())
            .build()
    }

    /**
     * WebSocket client: auth is via `access_token` query param (not the
     * Bearer interceptor). Read timeout must be 0 so idle STOMP connections
     * aren't killed between heartbeats; OkHttp ping keeps Cloudflare/nginx
     * from dropping the upgrade.
     */
    fun provideWebSocketOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    private fun agentPortalRetrofit(tokenStore: TokenStore): Retrofit {
        val baseUrl = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient(tokenStore))
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
    }

    fun provideAgentPortalApi(tokenStore: TokenStore): AgentPortalApi {
        return agentPortalRetrofit(tokenStore).create(AgentPortalApi::class.java)
    }

    /** Same backend/auth as AgentPortalApi; kept as a separate Retrofit interface (DeviceApi.kt). */
    fun provideDeviceApi(tokenStore: TokenStore): DeviceApi {
        return agentPortalRetrofit(tokenStore).create(DeviceApi::class.java)
    }

    /**
     * Client + Retrofit instance for the CSS auth server. Deliberately does
     * not attach the bearer-token interceptor: this is the login call, so
     * there usually isn't a token yet, and the auth server is a different
     * host than this app's own backend anyway. Every AuthApi call supplies
     * a fully-qualified @Url, so the base URL below is a Retrofit-mandated
     * placeholder that is never actually requested.
     */
    fun provideAuthApi(): AuthApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://auth-base-url-placeholder.invalid/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
        return retrofit.create(AuthApi::class.java)
    }
}
