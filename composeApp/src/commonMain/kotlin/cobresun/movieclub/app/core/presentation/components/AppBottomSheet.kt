package cobresun.movieclub.app.core.presentation.components

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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Platform look of a sheet's top edge: web/Material on Android, UIKit sheet metrics on iOS. */
data class SheetChrome(val cornerRadius: Dp, val grabberWidth: Dp, val grabberHeight: Dp, val grabberAreaHeight: Dp)

expect val platformSheetChrome: SheetChrome

/** Sheet state for [AppBottomSheet]; single-state sheets (the web's default) skip the half-open stop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppSheetState(skipPartiallyExpanded: Boolean = true): SheetState =
    rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

/**
 * The app's one bottom sheet, the native counterpart of the web's `VBottomSheet`:
 * rounded top, a centred grabber, at most 90% of the window tall, and lifted above
 * the keyboard and navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberAppSheetState(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    sheetGesturesEnabled: Boolean = true,
    /** Like web's `transparentHandle`: the grabber floats over content (e.g. a hero image) that reaches the top edge. */
    transparentHandle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val chrome = platformSheetChrome
    val maxHeight = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.9f).toDp() }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = RoundedCornerShape(topStart = chrome.cornerRadius, topEnd = chrome.cornerRadius),
        containerColor = containerColor,
        dragHandle = if (transparentHandle) null else ({ SheetGrabber(chrome) }),
        contentWindowInsets = {
            WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        },
    ) {
        // The cap goes on the content: the sheet's own modifier also sizes its full-screen scrim.
        if (transparentHandle) {
            Box {
                Column(Modifier.heightIn(max = maxHeight), content = content)
                SheetGrabber(chrome, Modifier.align(Alignment.TopCenter), overlay = true)
            }
        } else {
            Column(Modifier.heightIn(max = maxHeight - chrome.grabberAreaHeight), content = content)
        }
    }
}

@Composable
private fun SheetGrabber(chrome: SheetChrome, modifier: Modifier = Modifier, overlay: Boolean = false) {
    Box(modifier.fillMaxWidth().heightIn(min = chrome.grabberAreaHeight), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(chrome.grabberWidth, chrome.grabberHeight)
                .background(
                    // Over artwork the grabber needs to be lighter to stay visible.
                    if (overlay) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(50),
                ),
        )
    }
}
