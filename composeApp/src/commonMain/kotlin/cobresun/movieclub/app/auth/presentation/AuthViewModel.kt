package cobresun.movieclub.app.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()
        .onStart {
            _state.update {
                it.copy(user = AsyncResult.Success(authRepository.user))
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    fun onAction(action: AuthAction) = when (action) {
        is AuthAction.LogIn -> login(action.email, action.password)
    }

    private fun login(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(user = AsyncResult.Loading) }

        authRepository.login(email, password)
            .onSuccess { user ->
                _state.update { it.copy(user = AsyncResult.Success(user)) }
            }
            .onError { error ->
                _state.update { it.copy(user = AsyncResult.Error()) }
            }
    }
}

data class AuthState(
    val user: AsyncResult<User?> = AsyncResult.Loading
)
