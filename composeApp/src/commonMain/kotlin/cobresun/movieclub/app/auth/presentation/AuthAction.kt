package cobresun.movieclub.app.auth.presentation

sealed class AuthAction {
    data class LogIn(val email: String, val password: String) : AuthAction()
}
