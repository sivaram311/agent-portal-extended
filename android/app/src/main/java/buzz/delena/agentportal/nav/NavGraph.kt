package buzz.delena.agentportal.nav

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import buzz.delena.agentportal.AgentPortalApplication
import buzz.delena.agentportal.ui.screens.ChatScreen
import buzz.delena.agentportal.ui.screens.LoginScreen
import buzz.delena.agentportal.ui.screens.SessionListScreen
import buzz.delena.agentportal.ui.viewmodel.AuthViewModel
import buzz.delena.agentportal.ui.viewmodel.ChatViewModel
import buzz.delena.agentportal.ui.viewmodel.SessionListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Nav graph wired to real screens (ui.screens) via thin ViewModels
 * (ui.viewmodel) that talk to the AppContainer's repositories/STOMP client.
 * Route names are the contract in Routes.kt.
 */
@Composable
fun AgentPortalNavHost(
    navController: NavHostController = rememberNavController(),
    // css-next's OAuth redirect (buzz.delena.agentportal://oauth/callback)
    // lands on MainActivity.onNewIntent, which bridges it in here since the
    // AuthViewModel that started the flow lives in the LOGIN
    // NavBackStackEntry's ViewModelStore, not reachable from the Activity
    // directly. Defaults to an always-empty flow so Previews/tests that
    // don't care about SSO don't need to supply one.
    pendingOAuthIntent: StateFlow<Intent?> = MutableStateFlow(null),
) {
    val container = (LocalContext.current.applicationContext as AgentPortalApplication).container
    val startDestination = if (container.authRepository.isLoggedIn()) Routes.SESSION_LIST else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(container.authRepository))
            val state by viewModel.state.collectAsState()

            val oauthIntent by pendingOAuthIntent.collectAsState()
            LaunchedEffect(oauthIntent) {
                oauthIntent?.let { intent ->
                    viewModel.completeSsoLogin(intent) {
                        navController.navigate(Routes.SESSION_LIST) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                    (pendingOAuthIntent as? MutableStateFlow)?.value = null
                }
            }

            LoginScreen(
                state = state,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onSubmit = {
                    viewModel.submit {
                        navController.navigate(Routes.SESSION_LIST) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                },
                onSsoSuccess = {
                    navController.navigate(Routes.SESSION_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SESSION_LIST) {
            val viewModel: SessionListViewModel = viewModel(
                factory = SessionListViewModel.Factory(container.sessionRepository),
            )
            val state by viewModel.state.collectAsState()
            SessionListScreen(
                state = state,
                onSessionClick = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                onCreateSession = {
                    // Skeleton default: "demo" workspace (see agent-portal/workspaces/demo).
                    // A real create-session dialog (workspace picker, provider choice,
                    // presets) is a documented fast-follow, not part of this skeleton.
                    viewModel.createSession(
                        workspacePath = "demo",
                        title = null,
                        provider = null,
                        onCreated = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                    )
                },
                onRefresh = viewModel::refresh,
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    sessionId = sessionId,
                    initialTitle = "",
                    sessionRepository = container.sessionRepository,
                    stompClient = container.stompClient,
                    appContext = LocalContext.current.applicationContext,
                ),
            )
            val state by viewModel.state.collectAsState()
            ChatScreen(
                state = state,
                onPromptChange = viewModel::onPromptChange,
                onSendPrompt = viewModel::sendPrompt,
                onApprovePermission = viewModel::approvePermission,
                onRejectPermission = viewModel::rejectPermission,
                onDismissError = viewModel::dismissError,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
