package buzz.delena.agentportal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import buzz.delena.agentportal.AgentPortalApplication
import buzz.delena.agentportal.theme.AgentPortalTheme
import buzz.delena.agentportal.theme.ApColors
import buzz.delena.agentportal.ui.viewmodel.AuthViewModel

/** UI state for [LoginScreen]. Owned by a future AuthViewModel; this file has no ViewModel/network reference. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    // Optional: NavGraph.kt does not currently pass this (it wires only the
    // password lane's onSubmit), so this defaults to a no-op and the SSO
    // token exchange still runs and persists tokens via AuthViewModel either
    // way -- only the post-login navigation callback is a no-op until a caller
    // supplies one. See the AuthViewModel obtained below for why this screen
    // can drive the SSO flow without NavGraph passing it anything new.
    onSsoSuccess: () -> Unit = {},
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // Same ViewModelStoreOwner (the LOGIN NavBackStackEntry) that NavGraph.kt
    // already requests an AuthViewModel from for the password lane, so this
    // resolves to the exact same instance and shares state/isLoading/error --
    // not a second, disconnected ViewModel. Kept self-contained here (rather
    // than threaded through as a new required LoginScreen parameter) so
    // nav/NavGraph.kt does not need to change.
    val context = LocalContext.current
    val container = (context.applicationContext as AgentPortalApplication).container
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(container.authRepository))

    val ssoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // Best-effort: covers browsers/Custom Tab providers that do return the
        // redirect through the normal activity-result channel. The
        // authoritative path for buzz.delena.agentportal://oauth/callback is
        // the BROWSABLE intent-filter on MainActivity (singleTask), whose
        // onNewIntent is outside this file's scope -- forwarding that intent
        // to authViewModel.completeSsoLogin(intent, onSsoSuccess) is required
        // elsewhere for SSO to complete end to end.
        val data = result.data
        if (data != null) {
            authViewModel.completeSsoLogin(data, onSsoSuccess)
        }
    }

    Scaffold(containerColor = ApColors.Background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Agent Portal",
                    style = MaterialTheme.typography.titleLarge,
                    color = ApColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Supervise your coding agents, anywhere",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ApColors.TextMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ApColors.Surface),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = state.username,
                            onValueChange = onUsernameChange,
                            label = { Text("Username") },
                            singleLine = true,
                            enabled = !state.isLoading,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                            ),
                            colors = loginFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(14.dp))

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = { Text("Password") },
                            singleLine = true,
                            enabled = !state.isLoading,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                            ),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = ApColors.TextMuted,
                                    )
                                }
                            },
                            colors = loginFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (state.error != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp),
                            ) {
                                Text(
                                    text = state.error,
                                    color = ApColors.Danger,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(20.dp))

                        Button(
                            onClick = onSubmit,
                            enabled = !state.isLoading && state.username.isNotBlank() && state.password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApColors.Accent,
                                contentColor = ApColors.Background,
                                disabledContainerColor = ApColors.SurfaceAlt,
                                disabledContentColor = ApColors.TextMuted,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(48.dp),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = ApColors.Background,
                                )
                            } else {
                                Text("Sign in", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(
                            onClick = {
                                authViewModel.startSsoLogin(context) { intent ->
                                    ssoLauncher.launch(intent)
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                        ) {
                            Text("Sign in with SSO", color = ApColors.TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun loginFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = ApColors.SurfaceAlt,
    unfocusedContainerColor = ApColors.SurfaceAlt,
    disabledContainerColor = ApColors.SurfaceAlt,
    focusedIndicatorColor = ApColors.Accent,
    unfocusedIndicatorColor = ApColors.Border,
    cursorColor = ApColors.Accent,
    focusedTextColor = ApColors.TextPrimary,
    unfocusedTextColor = ApColors.TextPrimary,
    focusedLabelColor = ApColors.Accent,
    unfocusedLabelColor = ApColors.TextMuted,
)

@Preview(showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun LoginScreenPreview() {
    AgentPortalTheme {
        LoginScreen(
            state = LoginUiState(username = "dev", password = "hunter2"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Loading + error")
@Composable
private fun LoginScreenLoadingErrorPreview() {
    AgentPortalTheme {
        LoginScreen(
            state = LoginUiState(
                username = "dev",
                password = "wrong-pass",
                isLoading = true,
                error = "Invalid username or password",
            ),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}
