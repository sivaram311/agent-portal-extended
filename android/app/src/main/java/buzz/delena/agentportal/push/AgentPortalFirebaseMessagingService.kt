package buzz.delena.agentportal.push

import android.util.Log
import buzz.delena.agentportal.AgentPortalApplication
import buzz.delena.agentportal.core.data.PushTokenRegistrar
import buzz.delena.agentportal.notifications.PermissionApprovalNotifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AgentPortalFcmService"

/**
 * Push-notification entry point for FCM. Not yet invoked by the Android
 * system: no Firebase project is provisioned (no google-services.json, and
 * the com.google.gms.google-services Gradle plugin isn't applied), and
 * Firebase's own init sequence gates whether this service class is ever
 * instantiated -- so it is safe to write real logic here now, it simply
 * won't run until that's provisioned. It is deliberately NOT registered in
 * AndroidManifest.xml yet, for the same reason: declaring the standard
 * service and intent-filter block without the plugin present could either
 * be inert or trip manifest-merger issues depending on what the (currently
 * absent) plugin expects to inject.
 *
 * Manifest registration is a one-line addition once Firebase is
 * provisioned: add this inside the application block, as a sibling of the
 * existing MainActivity activity entry.
 *
 * <service
 *     android:name=".push.AgentPortalFirebaseMessagingService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT" />
 *     </intent-filter>
 * </service>
 */
class AgentPortalFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token issued; registering with backend")

        val registrar = PushTokenRegistrar(
            (applicationContext as AgentPortalApplication).container.deviceApi,
        )
        // FirebaseMessagingService callbacks aren't suspend functions, so a
        // plain IO-dispatched coroutine (rather than goAsync(), which is a
        // BroadcastReceiver-specific mechanism) is the standard way to do
        // async work here.
        CoroutineScope(Dispatchers.IO).launch {
            registrar.register(token).onFailure { t ->
                Log.w(TAG, "Failed to register FCM token with backend", t)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val sessionId = data["sessionId"]
        val eventType = data["eventType"]
        if (sessionId == null || eventType == null) {
            Log.w(TAG, "Received FCM data message missing sessionId/eventType; ignoring")
            return
        }

        if (eventType == "input_required") {
            val permissionId = data["permissionId"]
            if (permissionId == null) {
                Log.w(TAG, "input_required push for session $sessionId missing permissionId; ignoring")
                return
            }
            val toolLabel = data["toolLabel"] ?: "Tool permission"
            val detail = data["detail"]
            PermissionApprovalNotifier.postPermissionNotification(
                context = applicationContext,
                sessionId = sessionId,
                permissionId = permissionId,
                toolLabel = toolLabel,
                detail = detail,
            )
        } else {
            Log.d(TAG, "Received FCM data message for session $sessionId, eventType=$eventType (no handling wired for this type yet)")
        }
    }
}
