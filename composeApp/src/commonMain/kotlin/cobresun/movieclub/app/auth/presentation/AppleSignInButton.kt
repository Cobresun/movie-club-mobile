package cobresun.movieclub.app.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cobresun.movieclub.app.auth.domain.AppleCredential

sealed interface AppleSignInOutcome {
    data class Success(val credential: AppleCredential) : AppleSignInOutcome
    data object Cancelled : AppleSignInOutcome
    data class Failed(val reason: String?) : AppleSignInOutcome
}

/**
 * Platform "Sign in with Apple" button. iOS shows the native
 * `ASAuthorizationAppleIDButton` and runs the system authorization sheet.
 * Android renders nothing: Apple has no native SDK there, and the web flow
 * would need a server-side deep-link callback that does not exist yet.
 */
@Composable
expect fun AppleSignInButton(
    enabled: Boolean,
    onOutcome: (AppleSignInOutcome) -> Unit,
    modifier: Modifier = Modifier,
)
