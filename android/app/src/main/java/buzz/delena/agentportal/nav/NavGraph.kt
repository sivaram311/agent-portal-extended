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
import buzz.delena.agentportal.notifications.EXTRA_OPEN_SESSION_ID
import buzz.delena.agentportal.ui.screens.ChatScreen
import buzz.delena.agentportal.ui.screens.LoginScreen
import buzz.delena.agentportal.ui.screens.SessionListScreen
import buzz.delena.agentportal.ui.viewmodel.AuthViewModel
import buzz.delena.agentportal.ui.viewmodel.ChatViewModel
import buzz.delena.agentportal.ui.viewmodel.SessionListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AgentPortalNavHost(
    navController: NavHostController = rememberNavController(),
    pendingOAuthIntent: StateFlow<Intent?> = MutableStateFlow(null),
    pendingOpenSessionId: StateFlow<String?> = MutableStateFlow(null),
) {
    val container = (LocalContext.current.applicationContext as AgentPortalApplication).container
    val startDestination = if (container.authRepository.isLoggedIn()) Routes.SESSION_LIST else Routes.LOGIN

    val openSessionId by pendingOpenSessionId.collectAsState()
    LaunchedEffect(openSessionId) {
        val sessionId = openSessionId ?: return@LaunchedEffect
        if (container.authRepository.isLoggedIn()) {
            navController.navigate(Routes.chat(sessionId)) {
                launchSingleTop = true
            }
        }
        (pendingOpenSessionId as? MutableStateFlow)?.value = null
    }

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
                onCreateSession = { provider ->
                    viewModel.createSession(
                        workspacePath = "demo",
                        title = null,
                        provider = provider,
                        onCreated = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                    )
                },
                onFilterChange = viewModel::setFilter,
                onRefresh = viewModel::refresh,
                onDismissError = viewModel::dismissError,
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
                    onArchived = {
                        navController.popBackStack(Routes.SESSION_LIST, inclusive = false)
                    },
                ),
            )
            val state by viewModel.state.collectAsState()
            ChatScreen(
                state = state,
                onPromptChange = viewModel::onPromptChange,
                onSendPrompt = viewModel::sendPrompt,
                onOpenDecisionSheet = viewModel::openDecisionSheet,
                onDismissSheet = viewModel::dismissSheet,
                onOpenToolsSheet = viewModel::openToolsSheet,
                onOpenChangesSheet = viewModel::openChangesSheet,
                onSelectTool = viewModel::selectTool,
                onSelectChange = viewModel::selectChange,
                onAcceptChange = viewModel::acceptChange,
                onRejectChange = viewModel::rejectChange,
                onAllowOnce = viewModel::allowOnce,
                onAllowAlways = viewModel::allowAlways,
                onReject = viewModel::reject,
                onAcceptPlan = viewModel::acceptPlan,
                onRejectPlan = viewModel::rejectPlan,
                onCancelRun = viewModel::cancelRun,
                onArchive = viewModel::archive,
                onDismissError = viewModel::dismissError,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
