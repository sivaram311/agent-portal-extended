package buzz.delena.agentportal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import buzz.delena.agentportal.AgentPortalApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "PermissionActionReceiver"

/**
 * Handles the Approve/Reject actions attached to the notification posted by
 * PermissionApprovalNotifier, letting the user decide a pending tool
 * permission without opening the app. Registered (exported=false) in
 * AndroidManifest.xml -- this is a plain local broadcast receiver, it has no
 * dependency on Firebase/push being wired up.
 */
class PermissionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val permissionId = intent.getStringExtra(EXTRA_PERMISSION_ID)
        val decision = intent.getStringExtra(EXTRA_DECISION)

        if (sessionId == null || permissionId == null || decision == null) {
            Log.w(TAG, "Missing extras on permission decision broadcast; ignoring")
            return
        }

        // onReceive isn't a suspend context, and the receiver instance may
        // be torn down as soon as this method returns -- goAsync() plus
        // pendingResult.finish() is the standard pattern to keep the
        // process alive long enough for the async network call to finish.
        val pendingResult = goAsync()
        val sessionRepository = (context.applicationContext as AgentPortalApplication).container.sessionRepository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                sessionRepository.decidePermission(
                    sessionId = sessionId,
                    permissionId = permissionId,
                    decision = decision,
                    reason = null,
                ).onSuccess {
                    PermissionApprovalNotifier.cancelPermissionNotification(context, permissionId)
                }.onFailure { t ->
                    Log.w(TAG, "Failed to submit permission decision for $permissionId", t)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
