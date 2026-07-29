package buzz.delena.agentportal

import android.content.Context
import buzz.delena.agentportal.core.data.AuthRepository
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.TokenStore
import buzz.delena.agentportal.core.data.local.AppDatabase
import buzz.delena.agentportal.core.network.NetworkModule
import buzz.delena.agentportal.core.network.StompWebSocketClient

/**
 * Manual composition root. No DI framework (Hilt/Koin) is wired into this
 * module yet -- see NetworkModule's own note; this class is the single place
 * that would change if one is added later. Held as a singleton on
 * AgentPortalApplication, constructed once from application Context.
 */
class AppContainer(context: Context) {

    val tokenStore = TokenStore(context)

    private val agentPortalApi = NetworkModule.provideAgentPortalApi(tokenStore)
    private val authApi = NetworkModule.provideAuthApi()
    val deviceApi = NetworkModule.provideDeviceApi(tokenStore)

    private val database = AppDatabase.getInstance(context)

    val authRepository = AuthRepository(agentPortalApi, authApi, tokenStore)
    val sessionRepository = SessionRepository(agentPortalApi, database.sessionDao(), database.messageDao())

    // Separate OkHttp client for WS: infinite read timeout + protocol pings.
    // REST keeps long (but finite) timeouts for ACP handshake on /prompt.
    val stompClient = StompWebSocketClient(
        okHttpClient = NetworkModule.provideWebSocketOkHttpClient(),
        wsBaseUrl = BuildConfig.WS_BASE_URL,
        tokenStore = tokenStore,
    )
}
