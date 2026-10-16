package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The screen's snackbar state, so modal sheets (separate windows) can show messages too. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.small,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.primary,
            dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

/** Mirrors the screen snackbar at the bottom of a modal sheet. */
@Composable
fun BoxScope.SheetSnackbarHost() {
    val state = LocalSnackbarHostState.current ?: return
    AppSnackbarHost(
        state,
        Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    )
}
