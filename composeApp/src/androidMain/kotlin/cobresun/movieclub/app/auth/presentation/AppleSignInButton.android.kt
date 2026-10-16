package cobresun.movieclub.app.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppleSignInButton(
    enabled: Boolean,
    onOutcome: (AppleSignInOutcome) -> Unit,
    modifier: Modifier,
) = Unit
