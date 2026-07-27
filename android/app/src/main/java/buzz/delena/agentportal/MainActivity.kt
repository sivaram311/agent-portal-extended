package buzz.delena.agentportal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import buzz.delena.agentportal.nav.AgentPortalNavHost
import buzz.delena.agentportal.notifications.EXTRA_OPEN_SESSION_ID
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.ui.components.AppLockGate
import kotlinx.coroutines.flow.MutableStateFlow

// FragmentActivity (not plain ComponentActivity) is required here because
// androidx.biometric.BiometricPrompt's constructor needs a FragmentActivity
// (or a Fragment). FragmentActivity extends ComponentActivity, so
// setContent { ... } from androidx.activity.compose still works unchanged --
// this only widens the base class, it doesn't remove anything.
class MainActivity : FragmentActivity() {

    private val pendingOAuthIntent = MutableStateFlow<Intent?>(null)
    private val pendingOpenSessionId = MutableStateFlow<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AgentPortalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val hasSession = (application as AgentPortalApplication)
                        .container.tokenStore.hasAccessToken()
                    AppLockGate(hasSession = hasSession) {
                        AgentPortalNavHost(
                            pendingOAuthIntent = pendingOAuthIntent,
                            pendingOpenSessionId = pendingOpenSessionId,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.data?.scheme == "buzz.delena.agentportal") {
            pendingOAuthIntent.value = intent
        }
        intent.getStringExtra(EXTRA_OPEN_SESSION_ID)?.let { sessionId ->
            pendingOpenSessionId.value = sessionId
            intent.removeExtra(EXTRA_OPEN_SESSION_ID)
        }
    }
}
