package buzz.delena.agentportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.SessionRepository
import buzz.delena.agentportal.core.data.local.SessionEntity
import buzz.delena.agentportal.ui.screens.SessionListItem
import buzz.delena.agentportal.ui.screens.SessionListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionListViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _state = MutableStateFlow(SessionListUiState(isLoading = true))
    val state: StateFlow<SessionListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeSessions().collect { entities ->
                _state.value = _state.value.copy(
                    sessions = entities.map { it.toListItem() },
                    isLoading = false,
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            runCatching { sessionRepository.refreshSessions() }
            _state.value = _state.value.copy(isRefreshing = false, isLoading = false)
        }
    }

    fun createSession(workspacePath: String, title: String?, provider: String?, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            sessionRepository.createSession(workspacePath, title, provider)
                .onSuccess { onCreated(it.id) }
        }
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
