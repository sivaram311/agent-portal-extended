package buzz.delena.agentportal.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val TAG = "PermissionApprovalNotif"
private const val CHANNEL_ID = "permission_approvals"
private const val CHANNEL_NAME = "Permission approvals"

const val EXTRA_SESSION_ID = "buzz.delena.agentportal.extra.SESSION_ID"
const val EXTRA_PERMISSION_ID = "buzz.delena.agentportal.extra.PERMISSION_ID"
const val EXTRA_DECISION = "buzz.delena.agentportal.extra.DECISION"
/** Tap-notification deep link into Chat for the happy-path supervisor loop. */
const val EXTRA_OPEN_SESSION_ID = "buzz.delena.agentportal.extra.OPEN_SESSION_ID"

// Mirrors PermissionStatus's entry names (core/network/dto/PermissionDto.kt).
// decidePermission treats "decision" as a free string matching one of those
// entries, per PermissionDecisionRequest.kt's own note -- duplicated here as
// plain constants (rather than importing the network DTO enum) to keep this
// notification-plumbing package decoupled from core/network.
const val DECISION_APPROVE = "ALLOW_ONCE"
const val DECISION_REJECT = "REJECT_ONCE"

/**
 * Posts the system-notification counterpart of ChatScreen's in-app
 * PermissionRequestCard: lets the human approve or reject a pending
 * tool-permission request straight from the lock screen without opening the
 * app, which is the whole point of supervising a session from a phone (see
 * that card's own KDoc in ui/screens/ChatScreen.kt for the same framing).
 *
 * This works today for a foreground/backgrounded-but-alive app process. It
 * does NOT need Firebase to function in that case -- Firebase only extends
 * this to work when the app process is fully killed, which is an upgrade
 * path (see AgentPortalFirebaseMessagingService), not a prerequisite.
 */
object PermissionApprovalNotifier {

    fun postPermissionNotification(
        context: Context,
        sessionId: String,
        permissionId: String,
        toolLabel: String,
        detail: String?,
    ) {
        // Android 13+ (API 33) requires POST_NOTIFICATIONS to be granted at
        // runtime, not just declared in the manifest. This function has no
        // guaranteed Activity context to request it from, so on API 33+ if
        // it isn't already granted we just skip posting and log, rather
        // than crash or attempt a permission request from here.
        //
        // Follow-up (not solved in this file): something with real Activity
        // context -- most naturally MainActivity, owned by the biometric-
        // lock workstream, not this one -- needs to actually request
        // POST_NOTIFICATIONS at runtime on API 33+ so this path stops being
        // a silent no-op on new installs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping notification for permission $permissionId")
            return
        }

        ensureChannel(context)

        val notificationId = permissionId.hashCode()
        val approveIntent = decisionPendingIntent(context, sessionId, permissionId, DECISION_APPROVE, notificationId)
        val rejectIntent = decisionPendingIntent(context, sessionId, permissionId, DECISION_REJECT, notificationId)

        val openChatIntent = Intent(context, buzz.delena.agentportal.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SESSION_ID, sessionId)
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Needs you")
            .setContentText(toolLabel)
            .setContentIntent(contentPending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(0, "Reject", rejectIntent)
            .addAction(0, "Approve", approveIntent)

        if (detail != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$toolLabel\n\n$detail"))
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun cancelPermissionNotification(context: Context, permissionId: String) {
        NotificationManagerCompat.from(context).cancel(permissionId.hashCode())
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tool-permission approval requests from active agent sessions"
        }
        manager.createNotificationChannel(channel)
    }

    private fun decisionPendingIntent(
        context: Context,
        sessionId: String,
        permissionId: String,
        decision: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, PermissionActionReceiver::class.java).apply {
            // A distinct action string (rather than relying on requestCode
            // alone) guarantees this PendingIntent is never coalesced with
            // the approve/reject intent of a different pending permission,
            // or with the opposite decision for the same one.
            action = "buzz.delena.agentportal.action.PERMISSION_DECISION.$decision.$permissionId"
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_PERMISSION_ID, permissionId)
            putExtra(EXTRA_DECISION, decision)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
