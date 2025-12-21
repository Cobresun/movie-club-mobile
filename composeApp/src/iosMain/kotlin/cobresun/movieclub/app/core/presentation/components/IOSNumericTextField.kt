package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * iOS implementation of numeric text field.
 *
 * Uses KeyboardType.Text instead of KeyboardType.Decimal because iOS numeric keyboards
 * (UIKeyboardType.decimalPad, UIKeyboardType.numberPad) don't natively support Done buttons.
 * This is a fundamental iOS limitation.
 *
 * The text keyboard allows users to enter decimal numbers (e.g., "7.5") while providing
 * a functional Done button. When Done is pressed, the onDone callback is triggered and
 * the keyboard is explicitly hidden via SoftwareKeyboardController.
 *
 * **Trade-off**: Users see the full keyboard (letters + numbers) instead of a numeric-only
 * keyboard, but gain the ability to properly dismiss the keyboard.
 */
@Composable
actual fun PlatformNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    singleLine: Boolean,
    onDone: () -> Unit,
    focusRequester: FocusRequester
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDone()
                keyboardController?.hide()
            }
        ),
        singleLine = singleLine,
        cursorBrush = SolidColor(textStyle.color)
    )
}
