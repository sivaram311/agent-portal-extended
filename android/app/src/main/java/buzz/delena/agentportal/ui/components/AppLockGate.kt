package buzz.delena.agentportal.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
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
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors

// This gate protects a session that can run shell commands and edit files on
// a remote backend, so a lost/unlocked phone with a cached login token is
// real blast radius. Lock state is intentionally process-lifetime only
// (remember, not persisted) -- it re-locks on every cold app start rather
// than staying unlocked forever.
@Composable
fun AppLockGate(hasSession: Boolean, content: @Composable () -> Unit) {
    if (!hasSession) {
        // Nothing to protect yet -- user isn't logged in.
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }

    if (unlocked) {
        content()
        return
    }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val biometricManager = remember { BiometricManager.from(context) }
    val allowedAuthenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(allowedAuthenticators)
    }
    val deviceUnprotected = canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS

    fun launchPrompt() {
        if (deviceUnprotected) return

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Agent Portal")
            .setSubtitle("Authenticate to access your agent sessions")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or auth failed hard -- stay locked, the
                    // visible Unlock button lets them retry.
                }

                override fun onAuthenticationFailed() {
                    // A single failed attempt (e.g. bad fingerprint read);
                    // the system prompt keeps itself open for retries.
                }
            },
        )
        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        // Trigger automatically on first composition so the user doesn't
        // have to tap twice.
        if (!deviceUnprotected) {
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
                            text = "Device has no lock screen / biometric configured " +
                                "- app is unprotected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ApColors.Warning,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { unlocked = true }) {
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

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun AppLockGateLockedPreview() {
    AgentPortalTheme {
        // Preview only renders the locked chrome; biometric APIs need a real
        // FragmentActivity so the interactive prompt itself isn't exercised here.
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
