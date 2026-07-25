package buzz.delena.agentportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import buzz.delena.agentportal.core.data.AuthRepository
import buzz.delena.agentportal.ui.screens.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank() || current.isLoading) return

        _state.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.loginWithPassword(current.username, current.password)
            result.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
                onSuccess()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Sign-in failed",
                )
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
