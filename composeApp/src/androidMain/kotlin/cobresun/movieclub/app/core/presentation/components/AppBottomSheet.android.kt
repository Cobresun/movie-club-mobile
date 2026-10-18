package cobresun.movieclub.app.core.presentation.components

import androidx.compose.ui.layout.layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material sheet metrics, matching the web's `VBottomSheet`. */
private val cornerRadius = 16.dp
private val grabberWidth = 48.dp
private val grabberHeight = 6.dp
private val grabberAreaHeight = 32.dp

@OptIn(ExperimentalMaterial3Api::class)
private class MaterialSheetState(val state: SheetState) : AppSheetState {
    override val isVisible: Boolean get() = state.isVisible
    override suspend fun hide() = state.hide()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun rememberAppSheetState(confirmHide: () -> Boolean): AppSheetState {
    val currentConfirmHide by rememberUpdatedState(confirmHide)
    // Single-state sheets (the web's default) skip the half-open stop.
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || currentConfirmHide() },
    )
    return remember(state) { MaterialSheetState(state) }
}

/** Rounded top, a centred grabber, at most 90% of the window tall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: AppSheetState,
    containerColor: Color,
    sheetGesturesEnabled: Boolean,
    transparentHandle: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxFraction = 0.9f
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = (sheetState as MaterialSheetState).state,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        containerColor = containerColor,
        dragHandle = if (transparentHandle) null else ({ SheetGrabber() }),
        contentWindowInsets = {
            WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        },
    ) {
        // The cap goes on the content (the sheet's own modifier also sizes its full-screen
        // scrim) and is taken from the room the sheet is given, which already excludes the
        // keyboard: on iOS the sheet's layer shrinks above it rather than reporting ime insets.
        if (transparentHandle) {
            Box {
                Column(Modifier.capHeight(maxFraction, 0.dp), content = content)
                SheetGrabber(Modifier.align(Alignment.TopCenter), overlay = true)
            }
        } else {
            Column(Modifier.capHeight(maxFraction, grabberAreaHeight), content = content)
        }
    }
}

/** Caps the height at [fraction] of the incoming max height, less [reserved] (the grabber strip). */
private fun Modifier.capHeight(fraction: Float, reserved: Dp) = layout { measurable, constraints ->
    val cap = if (constraints.hasBoundedHeight) {
        ((constraints.maxHeight * fraction) - reserved.roundToPx()).toInt().coerceAtLeast(constraints.minHeight)
    } else {
        constraints.maxHeight
    }
    val placeable = measurable.measure(constraints.copy(maxHeight = cap))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@Composable
private fun SheetGrabber(modifier: Modifier = Modifier, overlay: Boolean = false) {
    Box(modifier.fillMaxWidth().heightIn(min = grabberAreaHeight), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(grabberWidth, grabberHeight)
                .background(
                    // Over artwork the grabber needs to be lighter to stay visible.
                    if (overlay) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(50),
                ),
        )
    }
}
