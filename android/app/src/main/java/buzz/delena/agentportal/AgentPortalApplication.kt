package buzz.delena.agentportal

import android.app.Application
import buzz.delena.agentportal.core.diagnostics.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process entry point. Installs crash diagnostics early; FCM is wired via
 * google-services when present.
 */
class AgentPortalApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.diagnosticsRepository.installCrashHandler()
        AppLog.i(TAG, "Application onCreate version=${BuildConfig.VERSION_NAME}")
        appScope.launch {
            container.diagnosticsRepository.uploadPendingCrashIfAny()
        }
    }

    private companion object {
        const val TAG = "AgentPortalApp"
    }
}
