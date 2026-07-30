package buzz.delena.agentportal.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

// Protects a cached login that can run shell commands on a remote backend.
// Lock is process-lifetime only (remember). Overlay keeps NavHost composition
// stable so unlocking does not recreate the whole graph mid-frame.
@Composable
fun AppLockGate(hasSession: Boolean, content: @Composable () -> Unit) {
    if (!hasSession) {
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleStarted by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleStarted = when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> false
                else -> lifecycleStarted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep content composed while locked so process death / unlock does not
        // tear down navigation mid-transition (felt like an unexpected close).
        content()

        if (!unlocked) {
            LockOverlay(
                activity = activity,
                lifecycleStarted = lifecycleStarted,
                onUnlocked = { unlocked = true },
            )
        }
    }
}

@Composable
private fun LockOverlay(
    activity: FragmentActivity?,
    lifecycleStarted: Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    val allowedAuthenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(allowedAuthenticators)
    }
    val deviceUnprotected = canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS || activity == null

    fun launchPrompt() {
        val host = activity ?: return
        if (deviceUnprotected || !lifecycleStarted) return
        if (!host.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Agent Portal")
            .setSubtitle("Authenticate to access your agent sessions")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        runCatching {
            BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        onUnlocked()
                    }
                },
            ).authenticate(promptInfo)
        }
    }

    LaunchedEffect(lifecycleStarted, activity) {
        if (!deviceUnprotected && lifecycleStarted) {
            launchPrompt()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ApColors.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                color = ApColors.Surface,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = ApColors.Accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Agent Portal is locked",
                        style = MaterialTheme.typography.titleLarge,
                        color = ApColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (deviceUnprotected) {
                        Text(
                            text = if (activity == null) {
                                "Could not open the device lock screen — tap Continue."
                            } else {
                                "Device has no lock screen / biometric configured " +
                                    "- app is unprotected"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ApColors.Warning,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onUnlocked) {
                            Text("Continue")
                        }
                    } else {
                        Text(
                            text = "App lock — your sign-in token is still saved. Authenticate to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ApColors.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { launchPrompt() }) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun AppLockGateLockedPreview() {
    AgentPortalTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(color = ApColors.Surface, shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = ApColors.Accent,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Agent Portal is locked",
                        style = MaterialTheme.typography.titleLarge,
                        color = ApColors.TextPrimary,
                    )
                }
            }
        }
    }
}
