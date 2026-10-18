package cobresun.movieclub.app.core.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * A small titled sheet with Cancel and a primary action: the native counterpart of
 * the web's `VModal`, which is a bottom sheet on phones. [content] sits between the
 * [message] and the buttons (e.g. a text field). Unless [dismissOnConfirm] is false
 * the sheet slides away before [onConfirm] runs; otherwise the caller closes it,
 * e.g. once a [loading] request finishes. While loading it can't be dismissed.
 */
@Composable
fun FormSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    dismissOnConfirm: Boolean = true,
    /** False for a one-button sheet, e.g. a "Done" acknowledgement. */
    showCancel: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val haptics = koinInject<HapticFeedback>()
    val busy by rememberUpdatedState(loading)
    val sheetState = rememberAppSheetState(confirmHide = { !busy })
    val scope = rememberCoroutineScope()
    fun hide(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }
    val confirm = {
        haptics.performConfirm()
        if (dismissOnConfirm) hide { onConfirm(); onDismiss() } else onConfirm()
    }
    AppBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = sheetState) {
        Column(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showCancel) TextButton(onClick = { hide(onDismiss) }, enabled = !loading) { Text("Cancel") }
                Button(
                    onClick = confirm,
                    enabled = confirmEnabled && !loading,
                    colors = if (destructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(confirmLabel)
                }
            }
        }
    }
}

/** A yes/no confirmation sheet; [destructive] styles the action in the error colour. */
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    loading: Boolean = false,
    dismissOnConfirm: Boolean = true,
) = FormSheet(
    title = title,
    message = message,
    confirmLabel = confirmLabel,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
    destructive = destructive,
    loading = loading,
    dismissOnConfirm = dismissOnConfirm,
)

/**
 * A single-field form sheet (rename a list, a category, the club...). [validate] runs
 * on every change; [serverError] shows until the user edits again; [transform]
 * sanitizes input as it's typed. The value is confirmed trimmed. With
 * [requireChange] the action stays disabled until the value differs from
 * [initialValue]. [prefix] previews the full result (e.g. the club URL) under the field.
 */
@Composable
fun TextInputSheet(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    label: String? = null,
    placeholder: String? = null,
    maxLength: Int? = null,
    isSaving: Boolean = false,
    serverError: String? = null,
    supportingText: String? = null,
    prefix: String? = null,
    validate: (String) -> String? = { if (it.isBlank()) "" else null },
    transform: (String) -> String = { it },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    requireChange: Boolean = false,
    /** False when the caller closes the sheet after a save request succeeds. */
    dismissOnConfirm: Boolean = true,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length))) }
    var edited by remember { mutableStateOf(false) }
    // An empty message marks the value invalid without showing an error line.
    val localError = if (edited) validate(value.text)?.takeIf { it.isNotEmpty() } else null
    val shownError = localError ?: serverError.takeIf { !edited }
    val valid = validate(value.text) == null && (!requireChange || value.text != initialValue)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // A new server error means the last submit failed: show it again.
    LaunchedEffect(serverError) { if (serverError != null) edited = false }
    val submit = { onConfirm(value.text.trim()) }

    FormSheet(
        title = title,
        confirmLabel = confirmLabel,
        onConfirm = submit,
        onDismiss = onDismiss,
        confirmEnabled = valid,
        loading = isSaving,
        dismissOnConfirm = dismissOnConfirm,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                val next = transform(it.text)
                if (maxLength == null || next.length <= maxLength) {
                    value = it.copy(text = next)
                    edited = true
                }
            },
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            enabled = !isSaving,
            isError = shownError != null,
            supportingText = (shownError ?: supportingText)?.let { t -> { Text(t) } },
            keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid && !isSaving && !dismissOnConfirm) submit() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        prefix?.let { p ->
            Text(
                p + value.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
