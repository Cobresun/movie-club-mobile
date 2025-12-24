package cobresun.movieclub.app.auth.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.app.AppTheme
import cobresun.movieclub.app.core.domain.AsyncResult
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreenRoot(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthorized: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.user) {
        if (state.user is AsyncResult.Success) {
            onAuthorized()
        }
    }

    AuthScreen(
        onAction = viewModel::onAction,
    )
}

@Composable
private fun AuthScreen(
    onAction: (AuthAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Auth",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            label = { Text(text = "Email") }
        )

        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            showPassword = showPassword,
            onShowPasswordToggle = { showPassword = !showPassword }
        )

        Button(
            onClick = {
                onAction(AuthAction.LogIn(email, password))
            }
        ) {
            Text(text = "Login")
        }
    }
}

@Composable
@Preview
private fun AuthScreenPreview() {
    AppTheme {
        AuthScreen(
            onAction = {},
        )
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(6.dp),
        label = { Text(text = "Password") },
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onShowPasswordToggle) {
                Icon(
                    imageVector = if (showPassword) {
                        Icons.Filled.Visibility
                    } else {
                        Icons.Filled.VisibilityOff
                    },
                    contentDescription = "Toggle password visibility"
                )
            }
        }
    )
}

@Composable
@Preview
private fun PasswordTextFieldPreview() {
    AppTheme {
        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            showPassword = showPassword,
            onShowPasswordToggle = { showPassword = !showPassword }
        )
    }
}

@Composable
@Preview
private fun PasswordTextFieldFilledPreview() {
    AppTheme {
        var password by remember { mutableStateOf("some password") }
        var showPassword by remember { mutableStateOf(false) }
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            showPassword = showPassword,
            onShowPasswordToggle = { showPassword = !showPassword }
        )
    }
}
