package cobresun.movieclub.app.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.presentation.components.FormSheet
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.core.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ForgotPasswordState(
    val isSending: Boolean = false,
    val sentTo: String? = null,
    val error: String? = null,
)

class ForgotPasswordViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordState())
    val state = _state.asStateFlow()

    fun send(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            accountRepository.requestPasswordReset(email)
                .onSuccess { _state.update { it.copy(isSending = false, sentTo = email.trim()) } }
                .onError { e -> _state.update { it.copy(isSending = false, error = e.toUserMessage("send the reset email")) } }
        }
    }

    fun reset() = _state.update { ForgotPasswordState() }
}

@Composable
fun ForgotPasswordSheet(
    initialEmail: String,
    onDismiss: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf(initialEmail) }
    val close = {
        viewModel.reset()
        onDismiss()
    }
    val sentTo = state.sentTo

    // One sheet that turns into the confirmation, rather than swapping sheets.
    FormSheet(
        title = if (sentTo == null) "Reset password" else "Check your email",
        message = if (sentTo == null) "Enter your account email and we'll send you a reset link."
        else "If an account exists for $sentTo, we've sent a link to reset your password.",
        confirmLabel = if (sentTo == null) "Send link" else "Done",
        showCancel = sentTo == null,
        confirmEnabled = sentTo != null || isPlausibleEmail(email),
        loading = state.isSending,
        dismissOnConfirm = sentTo != null,
        onConfirm = { if (sentTo == null) viewModel.send(email) },
        onDismiss = close,
    ) {
        if (sentTo == null) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.isSending,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
