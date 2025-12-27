package cobresun.movieclub.app.auth.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

enum class AuthMode {
    LOGIN, SIGNUP
}

@Composable
fun AuthScreenRoot(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthorized: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(state.user) {
        if (state.user is AsyncResult.Success) {
            onAuthorized()
        }
    }

    AuthLandingScreen(
        errorMessage = errorMessage,
        confirmationMessage = state.confirmationMessage,
        isLoading = state.user is AsyncResult.Loading,
        onAction = viewModel::onAction
    )
}

@Composable
private fun AuthLandingScreen(
    errorMessage: String?,
    confirmationMessage: String?,
    isLoading: Boolean,
    onAction: (AuthAction) -> Unit
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Movie Club",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Mode Toggle (FilterChips)
        ModeToggle(
            selectedMode = authMode,
            onModeChange = {
                authMode = it
                onAction(AuthAction.ClearError)
            },
            enabled = !isLoading
        )

        // Confirmation Message
        confirmationMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Animated Content Area
        AnimatedContent(
            targetState = authMode,
            transitionSpec = {
                slideInHorizontally { width ->
                    if (targetState == AuthMode.SIGNUP) width else -width
                } + fadeIn() togetherWith
                slideOutHorizontally { width ->
                    if (targetState == AuthMode.SIGNUP) -width else width
                } + fadeOut()
            }
        ) { mode ->
            when (mode) {
                AuthMode.LOGIN -> LoginForm(
                    isLoading = isLoading,
                    onAction = onAction
                )
                AuthMode.SIGNUP -> SignupForm(
                    isLoading = isLoading,
                    onAction = onAction
                )
            }
        }

        // Error Message
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ModeToggle(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMode == AuthMode.LOGIN,
            onClick = { onModeChange(AuthMode.LOGIN) },
            label = { Text("Login") },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == AuthMode.SIGNUP,
            onClick = { onModeChange(AuthMode.SIGNUP) },
            label = { Text("Sign Up") },
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    onAction: (AuthAction) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            enabled = !isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            showPassword = showPassword,
            onShowPasswordToggle = { showPassword = !showPassword },
            enabled = !isLoading
        )

        Button(
            onClick = {
                onAction(AuthAction.LogIn(email.trim(), password))
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logging in...")
            } else {
                Text("Log In")
            }
        }
    }
}

@Composable
private fun SignupForm(
    isLoading: Boolean,
    onAction: (AuthAction) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            enabled = !isLoading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            enabled = !isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            showPassword = showPassword,
            onShowPasswordToggle = { showPassword = !showPassword },
            enabled = !isLoading
        )

        // Password hint
        Text(
            text = "Password must be at least 6 characters",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                onAction(AuthAction.SignUp(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim()
                ))
            },
            enabled = !isLoading && email.isNotBlank() && password.length >= 6 && fullName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creating account...")
            } else {
                Text("Sign Up")
            }
        }
    }
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Password") },
        enabled = enabled,
        singleLine = true,
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
private fun AuthLandingScreenPreview() {
    AppTheme {
        AuthLandingScreen(
            errorMessage = null,
            confirmationMessage = null,
            isLoading = false,
            onAction = {}
        )
    }
}

@Composable
@Preview
private fun AuthLandingScreenWithErrorPreview() {
    AppTheme {
        AuthLandingScreen(
            errorMessage = "Sign-up failed. Please try again.",
            confirmationMessage = null,
            isLoading = false,
            onAction = {}
        )
    }
}

@Composable
@Preview
private fun AuthLandingScreenWithConfirmationPreview() {
    AppTheme {
        AuthLandingScreen(
            errorMessage = null,
            confirmationMessage = "A confirmation message was sent to your email, click the link there to continue.",
            isLoading = false,
            onAction = {}
        )
    }
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
