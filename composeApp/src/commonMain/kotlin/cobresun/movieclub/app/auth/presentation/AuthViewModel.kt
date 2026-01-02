package cobresun.movieclub.app.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _navigationEvents = Channel<AuthNavigationEvent>()
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        checkAuthentication()
    }

    fun onAction(action: AuthAction) = when (action) {
        is AuthAction.LogIn -> login(action.email, action.password)
        is AuthAction.SignUp -> signup(action.email, action.password, action.fullName)
        is AuthAction.Logout -> logout()
        is AuthAction.ClearError -> clearError()
        is AuthAction.ResendVerificationEmail -> resendVerificationEmail(action.email)
    }

    sealed interface AuthNavigationEvent {
        data object NavigateToLandingPage : AuthNavigationEvent
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
        _state.update { it.copy(user = AsyncResult.Loading, lastEmail = email) }
        _errorMessage.update { null }

        authRepository.login(email, password)
            .onSuccess { user ->
                _state.update { it.copy(user = AsyncResult.Success(Unit)) }
            }
            .onError { error ->
                _state.update { it.copy(user = AsyncResult.Error(dataError = error)) }
                _errorMessage.update {
                    when (error) {
                        DataError.Remote.EMAIL_NOT_CONFIRMED ->
                            "Please verify your email address before signing in. Check your inbox for a verification link."
                        DataError.Remote.NO_INTERNET ->
                            "No internet connection. Please check your connection and try again."
                        DataError.Remote.REQUEST_TIMEOUT ->
                            "Request timed out. Please try again."
                        DataError.Remote.SERVER ->
                            "Server error. Please try again later."
                        DataError.Remote.SERIALIZATION ->
                            "Invalid credentials. Please check your email and password."
                        else ->
                            "Login failed. Please check your credentials and try again."
                    }
                }
            }
    }

    private fun signup(email: String, password: String, fullName: String) = viewModelScope.launch {
        _state.update { it.copy(user = AsyncResult.Loading, confirmationMessage = null, lastEmail = email) }
        _errorMessage.update { null }

        authRepository.register(email, password, fullName)
            .onSuccess { user ->
                // Better Auth requires email verification before session is created
                _state.update {
                    it.copy(
                        user = AsyncResult.Error(dataError = null),
                        confirmationMessage = "Account created! Please check your email to verify your account before signing in."
                    )
                }
            }
            .onError { error ->
                _state.update { it.copy(user = AsyncResult.Error(dataError = error)) }
                _errorMessage.update {
                    when (error) {
                        DataError.Remote.NO_INTERNET ->
                            "No internet connection. Please check your connection and try again."
                        DataError.Remote.REQUEST_TIMEOUT ->
                            "Request timed out. Please try again."
                        DataError.Remote.SERVER ->
                            "Server error. Please try again later."
                        else ->
                            "Sign-up failed. Please try again or log in if you already have an account."
                    }
                }
            }
    }

    private fun logout() = viewModelScope.launch {
        _state.update { it.copy(isLoggingOut = true) }

        authRepository.logout()
            .onSuccess {
                _state.update { it.copy(user = AsyncResult.Error(dataError = null), isLoggingOut = false) }
                _navigationEvents.send(AuthNavigationEvent.NavigateToLandingPage)
            }
            .onError { error ->
                // Even if logout fails, clear auth state
                _state.update { it.copy(user = AsyncResult.Error(dataError = error), isLoggingOut = false) }
                _navigationEvents.send(AuthNavigationEvent.NavigateToLandingPage)
            }
    }

    private fun resendVerificationEmail(email: String) = viewModelScope.launch {
        _errorMessage.update { null }

        authRepository.sendVerificationEmail(email)
            .onSuccess {
                _state.update {
                    it.copy(confirmationMessage = "Verification email sent! Please check your inbox.")
                }
            }
            .onError { error ->
                _errorMessage.update {
                    when (error) {
                        DataError.Remote.NO_INTERNET ->
                            "No internet connection. Please check your connection and try again."
                        else ->
                            "Failed to send verification email. Please try again."
                    }
                }
            }
    }

    private fun clearError() {
        _errorMessage.update { null }
    }
}

data class AuthState(
    val user: AsyncResult<Unit> = AsyncResult.Loading,
    val confirmationMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val lastEmail: String? = null
)
