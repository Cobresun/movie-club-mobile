package cobresun.movieclub.app.core.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.abs

/**
 * While a text field inside has focus, a tap outside it or a drag of the
 * content puts the keyboard away (iOS `keyboardDismissMode`). A multi-line
 * field has no Done key (Return adds a line), so without this iOS has no way
 * to hide the keyboard.
 *
 * Drags are read from the finger itself, not nested scroll: focusing a field
 * scrolls it into view as user input, which would dismiss the keyboard at once.
 */
fun Modifier.dismissKeyboardOnTapOrDrag(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    var typing by remember { mutableStateOf(false) }
    // The chain keeps the same shape either way: inserting nodes on focus would
    // rebuild the ones below and drop the focus that just landed.
    onFocusChanged { typing = it.hasFocus }
        .pointerInput(typing) {
            if (typing) detectTapGestures { focusManager.clearFocus() }
        }
        .pointerInput(typing) {
            if (!typing) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes
                        .firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (abs(change.position.y - down.position.y) > viewConfiguration.touchSlop) {
                        focusManager.clearFocus()
                        break
                    }
                }
            }
        }
}
