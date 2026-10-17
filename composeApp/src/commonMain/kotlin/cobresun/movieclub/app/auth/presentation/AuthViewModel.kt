package cobresun.movieclub.app.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
        is AuthAction.AppleSignInResult -> onAppleSignInResult(action.outcome)
        is AuthAction.Logout -> logout()
        is AuthAction.ClearError -> clearError()
        is AuthAction.ResendVerificationEmail -> resendVerificationEmail(action.email)
    }

    sealed interface AuthNavigationEvent {
        data object NavigateToLandingPage : AuthNavigationEvent
    }

    private var verifyJob: Job? = null

    /**
     * A stored session paints the signed-in app straight away (web `useSessionHint`)
     * and is confirmed in the background. Being offline or the server being down
     * keeps you signed in and retries; only the server saying "no session" signs out.
     */
    private fun checkAuthentication() {
        viewModelScope.launch {
            if (authRepository.hasStoredSession()) {
                _state.update { it.copy(user = AsyncResult.Success(Unit)) }
                verifySession()
                return@launch
            }
            _state.update { it.copy(user = AsyncResult.Loading) }
            when (val result = authRepository.getSession()) {
                is Result.Success -> _state.update {
                    it.copy(user = if (result.data != null) AsyncResult.Success(Unit) else AsyncResult.Error(dataError = null))
                }
                is Result.Error -> _state.update { it.copy(user = AsyncResult.Error(dataError = result.error)) }
            }
        }
    }

    private fun verifySession() {
        verifyJob?.cancel()
        verifyJob = viewModelScope.launch {
            for (attempt in 0 until VERIFY_RETRY_DELAYS_MS.size + 1) {
                when (val result = authRepository.getSession()) {
                    is Result.Success -> {
                        if (result.data == null) sessionEnded()
                        return@launch
                    }
                    // Can't reach the server: stay signed in (screens show their own offline
                    // states with retry) and check again in a while.
                    is Result.Error -> VERIFY_RETRY_DELAYS_MS.getOrNull(attempt)?.let { delay(it) } ?: return@launch
                }
            }
        }
    }

    /** The server no longer knows this session (expired or revoked elsewhere). */
    private suspend fun sessionEnded() {
        authRepository.logout()
        _state.update { it.copy(user = AsyncResult.Error(dataError = null)) }
        _navigationEvents.send(AuthNavigationEvent.NavigateToLandingPage)
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

    private fun onAppleSignInResult(outcome: AppleSignInOutcome) {
        when (outcome) {
            is AppleSignInOutcome.Cancelled -> Unit
            is AppleSignInOutcome.Failed -> _errorMessage.update {
                "Sign in with Apple couldn't be completed. Please try again."
            }
            is AppleSignInOutcome.Success -> viewModelScope.launch {
                _state.update { it.copy(user = AsyncResult.Loading, confirmationMessage = null) }
                _errorMessage.update { null }

                authRepository.loginWithApple(outcome.credential)
                    .onSuccess {
                        _state.update { it.copy(user = AsyncResult.Success(Unit)) }
                    }
                    .onError { error ->
                        _state.update { it.copy(user = AsyncResult.Error(dataError = error)) }
                        _errorMessage.update {
                            when (error) {
                                DataError.Remote.NO_INTERNET ->
                                    "No internet connection. Please check your connection and try again."
                                DataError.Remote.REQUEST_TIMEOUT ->
                                    "Request timed out. Please try again."
                                else ->
                                    "Sign in with Apple isn't available right now. Please use email and password instead."
                            }
                        }
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
        verifyJob?.cancel()
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

private val VERIFY_RETRY_DELAYS_MS = listOf(3_000L, 10_000L, 30_000L, 60_000L, 120_000L, 300_000L)

data class AuthState(
    val user: AsyncResult<Unit> = AsyncResult.Loading,
    val confirmationMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val lastEmail: String? = null
)
