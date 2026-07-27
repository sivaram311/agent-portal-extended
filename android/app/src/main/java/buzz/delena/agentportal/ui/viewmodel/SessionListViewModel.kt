package buzz.delena.agentportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.SessionEntity
import buzz.delena.agentportal.ui.components.isNeedsYouStatus
import buzz.delena.agentportal.ui.components.isRunningStatus
import buzz.delena.agentportal.ui.screens.SessionListItem
import buzz.delena.agentportal.ui.screens.SessionListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SessionListFilter(val label: String) {
    ALL("All"),
    NEEDS_YOU("Needs you"),
    RUNNING("Running"),
    FAILED("Failed"),
}

class SessionListViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val filter = MutableStateFlow(SessionListFilter.ALL)
    private val loading = MutableStateFlow(true)
    private val refreshing = MutableStateFlow(false)
    private val creating = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private data class Meta(
        val filter: SessionListFilter,
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val isCreating: Boolean,
        val error: String?,
    )

    private val meta = combine(filter, loading, refreshing, creating, error) { f, l, r, c, e ->
        Meta(f, l, r, c, e)
    }

    val state: StateFlow<SessionListUiState> = combine(
        sessionRepository.observeSessions(),
        meta,
    ) { entities, m ->
        SessionListUiState(
            sessions = entities
                .filter { matches(it.status, m.filter) }
                .map { it.toListItem() },
            filter = m.filter,
            isLoading = m.isLoading,
            isRefreshing = m.isRefreshing,
            isCreating = m.isCreating,
            error = m.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionListUiState(isLoading = true),
    )

    init {
        refresh()
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
            runCatching { sessionRepository.refreshSessions() }
                .onFailure { error.value = it.message ?: "Couldn't refresh sessions" }
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
                .onFailure { error.value = it.message ?: "Couldn't create session" }
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

    class Factory(private val sessionRepository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionListViewModel(sessionRepository) as T
        }
    }
}
