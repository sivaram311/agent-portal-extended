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
    private val okHttpClient = NetworkModule.provideOkHttpClient(tokenStore)

    private val database = AppDatabase.getInstance(context)

    val authRepository = AuthRepository(agentPortalApi, authApi, tokenStore)
    val sessionRepository = SessionRepository(agentPortalApi, database.sessionDao(), database.messageDao())

    val stompClient = StompWebSocketClient(
        okHttpClient = okHttpClient,
        wsBaseUrl = BuildConfig.WS_BASE_URL,
        tokenStore = tokenStore,
    )
}
