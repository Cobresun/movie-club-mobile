package cobresun.movieclub.app.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    init {
        checkAuthentication()
    }

    fun onAction(action: AuthAction) = when (action) {
        is AuthAction.LogIn -> login(action.email, action.password)
        is AuthAction.Logout -> logout()
    }

    private fun checkAuthentication() {
        viewModelScope.launch {
            _state.update { it.copy(user = AsyncResult.Loading) }
            when (val result = authRepository.getUser()) {
                is Result.Success -> {
                    _state.update { it.copy(user = AsyncResult.Success(Unit)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(user = AsyncResult.Error(dataError = result.error)) }
                }
            }
        }
    }

    private fun login(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(user = AsyncResult.Loading) }

        authRepository.login(email, password)
            .onSuccess { user ->
                _state.update { it.copy(user = AsyncResult.Success(Unit)) }
            }
            .onError { error ->
                _state.update { it.copy(user = AsyncResult.Error(dataError = error)) }
            }
    }

    private fun logout() = viewModelScope.launch {
        authRepository.logout()
            .onSuccess {
                _state.update { it.copy(user = AsyncResult.Error(dataError = null)) }
            }
            .onError { error ->
                // Even if logout fails, clear auth state
                _state.update { it.copy(user = AsyncResult.Error(dataError = error)) }
            }
    }
}

data class AuthState(
    val user: AsyncResult<Unit> = AsyncResult.Loading
)
