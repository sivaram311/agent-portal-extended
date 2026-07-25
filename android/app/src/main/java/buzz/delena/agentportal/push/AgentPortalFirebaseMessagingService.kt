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
 * Push-notification entry point for FCM. A Firebase project is now
 * provisioned (google-services.json present, com.google.gms.google-services
 * plugin applied) and this service is registered in AndroidManifest.xml, so
 * it is live: onNewToken fires on install/token-rotation and registers with
 * the backend; onMessageReceived handles data messages while the app process
 * is alive (system-tray display for messages received while fully backgrounded
 * with the app killed is handled by Firebase itself for notification-type
 * payloads, not this method).
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
