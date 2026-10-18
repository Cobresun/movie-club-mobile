package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Handle on a shown [AppBottomSheet], so its owner can animate it away before acting. */
@Stable
interface AppSheetState {
    /** False once the sheet has been hidden, by [hide] or by the user. */
    val isVisible: Boolean

    /** Animates the sheet away; returns once it is gone. */
    suspend fun hide()
}

/** State for [AppBottomSheet]; [confirmHide] returning false blocks swipe and tap-outside dismissal. */
@Composable
expect fun rememberAppSheetState(confirmHide: () -> Boolean = { true }): AppSheetState

/**
 * The app's one bottom sheet, the native counterpart of the web's `VBottomSheet`:
 * sized to its content, at most nearly the window tall, and lifted above the keyboard
 * and navigation bar. Android draws a Material sheet; iOS presents a real UIKit sheet
 * (`UISheetPresentationController`) hosting the content, for the system's grabber,
 * motion, dimming and swipe-to-dismiss.
 */
@Composable
expect fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: AppSheetState = rememberAppSheetState(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    sheetGesturesEnabled: Boolean = true,
    /** Like web's `transparentHandle`: the grabber floats over content (e.g. a hero image) that reaches the top edge. */
    transparentHandle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
)
