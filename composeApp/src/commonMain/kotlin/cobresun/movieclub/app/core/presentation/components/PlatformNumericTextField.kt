package cobresun.movieclub.app.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle

/**
 * Platform-specific text field for numeric input with proper keyboard handling on both platforms.
 *
 * **Android**: Uses decimal keyboard (KeyboardType.Decimal) with native Done button
 * **iOS**: Uses text keyboard (KeyboardType.Text) with Done button, since iOS numeric keyboards
 * don't support Done buttons natively
 *
 * Both implementations support decimal number input (e.g., "7.5") and properly handle the Done action.
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier for the text field
 * @param textStyle Text styling
 * @param singleLine Whether to limit to single line
 * @param onDone Callback when Done is pressed
 * @param focusRequester FocusRequester to control focus
 */
@Composable
expect fun PlatformNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    singleLine: Boolean,
    onDone: () -> Unit,
    focusRequester: FocusRequester
)
