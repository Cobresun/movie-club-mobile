package cobresun.movieclub.app.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
            observeUserAccessToken()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    fun onAction(action: AuthAction) = when (action) {
        is AuthAction.LogIn -> login(action.email, action.password)
    }

    // TODO: Need to observe full Token object, and not just the access token.
    //  Implement refreshing of access token if its expired.
    private fun observeUserAccessToken() = viewModelScope.launch {
        authRepository.userAccessToken.collectLatest { token ->
            _state.update { it.copy(userAccessToken = AsyncResult.Success(token)) }
        }
    }

    private fun login(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(userAccessToken = AsyncResult.Loading) }

        authRepository.login(email, password)
            .onSuccess { user ->
                _state.update { it.copy(userAccessToken = AsyncResult.Success(user.token.accessToken)) }
            }
            .onError { error ->
                _state.update { it.copy(userAccessToken = AsyncResult.Error()) }
            }
    }
}

data class AuthState(
    val userAccessToken: AsyncResult<String?> = AsyncResult.Loading
)
