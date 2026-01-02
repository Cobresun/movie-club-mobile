package cobresun.movieclub.app.auth.presentation

sealed class AuthAction {
    data class LogIn(val email: String, val password: String) : AuthAction()
    data class SignUp(
        val email: String,
        val password: String,
        val fullName: String
    ) : AuthAction()
    data object Logout : AuthAction()
    data object ClearError : AuthAction()
    data class ResendVerificationEmail(val email: String) : AuthAction()
}
