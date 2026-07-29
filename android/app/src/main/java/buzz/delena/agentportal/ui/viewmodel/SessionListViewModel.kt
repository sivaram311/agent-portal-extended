package buzz.delena.agentportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.AuthRepository
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.SessionEntity
import buzz.delena.agentportal.core.network.userFacingErrorMessage
import buzz.delena.agentportal.ui.components.ConnectionStatusUi
import buzz.delena.agentportal.ui.components.isNeedsYouStatus
import buzz.delena.agentportal.ui.components.isRunningStatus
import buzz.delena.agentportal.ui.screens.SessionListItem
import buzz.delena.agentportal.ui.screens.SessionListUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SessionListFilter(val label: String) {
    ALL("All"),
    NEEDS_YOU("Needs you"),
    RUNNING("Running"),
    FAILED("Failed"),
}

class SessionListViewModel(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(SessionListFilter.ALL)
    private val loading = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(false)
    private val creating = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val connectionStatus = MutableStateFlow(ConnectionStatusUi.from(authRepository.authSessionInfo()))

    private data class Flags(
        val filter: SessionListFilter,
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val isCreating: Boolean,
        val error: String?,
    )

    private val flags = combine(filter, loading, refreshing, creating, error) { f, l, r, c, e ->
        Flags(f, l, r, c, e)
    }

    val state: StateFlow<SessionListUiState> = combine(
        sessionRepository.observeSessions(),
        flags,
        connectionStatus,
    ) { entities, f, connection ->
        SessionListUiState(
            sessions = entities
                .filter { matches(it.status, f.filter) }
                .map { it.toListItem() },
            filter = f.filter,
            isLoading = f.isLoading,
            isRefreshing = f.isRefreshing,
            isCreating = f.isCreating,
            error = f.error,
            connectionStatus = connection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionListUiState(isLoading = true),
    )

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                connectionStatus.value = ConnectionStatusUi.from(authRepository.authSessionInfo())
                delay(15_000)
            }
        }
    }

    fun setFilter(value: SessionListFilter) {
        filter.value = value
    }

    fun dismissError() {
        error.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            connectionStatus.value = ConnectionStatusUi.from(authRepository.authSessionInfo())
            runCatching { sessionRepository.refreshSessions() }
                .onFailure { error.value = userFacingErrorMessage(it) }
            refreshing.value = false
            loading.value = false
        }
    }

    fun createSession(workspacePath: String, title: String?, provider: String?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            creating.value = true
            error.value = null
            sessionRepository.createSession(workspacePath, title, provider)
                .onSuccess { onCreated(it.id) }
                .onFailure { error.value = userFacingErrorMessage(it) }
            creating.value = false
        }
    }

    private fun matches(status: String, selected: SessionListFilter): Boolean = when (selected) {
        SessionListFilter.ALL -> status.uppercase() != "ARCHIVED"
        SessionListFilter.NEEDS_YOU -> isNeedsYouStatus(status)
        SessionListFilter.RUNNING -> isRunningStatus(status)
        SessionListFilter.FAILED -> status.uppercase() in setOf("FAILED", "ERROR")
    }

    private fun SessionEntity.toListItem() = SessionListItem(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: workspacePath.substringAfterLast('/'),
        workspacePath = workspacePath,
        status = status,
        provider = provider,
        updatedAtLabel = updatedAt,
    )

    class Factory(
        private val sessionRepository: SessionRepository,
        private val authRepository: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionListViewModel(sessionRepository, authRepository) as T
        }
    }
}
