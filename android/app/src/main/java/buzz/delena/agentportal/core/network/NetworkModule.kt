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

    /** Client used for this app's own backend -- attaches the bearer token. */
    fun provideOkHttpClient(tokenStore: TokenStore): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor(tokenStore))
            .addInterceptor(loggingInterceptor())
            .build()
    }

    fun provideAgentPortalApi(tokenStore: TokenStore): AgentPortalApi {
        val baseUrl = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient(tokenStore))
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
        return retrofit.create(AgentPortalApi::class.java)
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
