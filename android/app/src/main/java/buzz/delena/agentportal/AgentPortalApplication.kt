package buzz.delena.agentportal

import android.app.Application

/**
 * Process entry point. Deliberately does NOT touch Firebase/FCM at startup:
 * the firebase-messaging dependency is present for the push-notification
 * track (see docs/ROADMAP.md), but no google-services.json exists yet and no
 * google-services Gradle plugin is applied, so calling FirebaseMessaging
 * APIs before that lands would crash at runtime. Wire it up only after a
 * Firebase project + google-services.json are provisioned.
 */
class AgentPortalApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
