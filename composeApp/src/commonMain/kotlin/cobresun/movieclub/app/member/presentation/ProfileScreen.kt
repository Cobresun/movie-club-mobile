package cobresun.movieclub.app.member.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.presentation.components.ConfirmDialog
import cobresun.movieclub.app.member.presentation.components.ErrorMessage
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.SettingsDivider
import cobresun.movieclub.app.member.presentation.components.SettingsGroup
import cobresun.movieclub.app.member.presentation.components.SettingsRow
import cobresun.movieclub.app.member.presentation.components.SettingsSectionHeader
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import cobresun.movieclub.app.member.presentation.components.TextInputDialog
import cobresun.movieclub.app.member.presentation.platform.rememberImagePicker
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreenRoot(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    isSigningOut: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(ProfileAction.OnClearMessage)
        }
    }
    val pickImage = rememberImagePicker { picked ->
        viewModel.onAction(ProfileAction.OnAvatarPicked(picked?.bytes, picked?.mimeType ?: "image/jpeg"))
    }

    Scaffold(
        topBar = { SettingsTopBar("Account", onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val member = state.member) {
                is AsyncResult.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is AsyncResult.Error -> ErrorMessage(
                    message = member.exception?.message ?: "Couldn't load your profile.",
                    onRetry = { viewModel.onAction(ProfileAction.Retry) },
                    modifier = Modifier.align(Alignment.Center),
                )
                is AsyncResult.Success -> ProfileContent(
                    member = member.data,
                    state = state,
                    onAction = viewModel::onAction,
                    onPickPhoto = pickImage,
                    onSignOut = onSignOut,
                    isSigningOut = isSigningOut,
                )
            }
        }
    }

    val member = (state.member as? AsyncResult.Success)?.data
    if (state.isEditingName && member != null) {
        TextInputDialog(
            title = "Display name",
            initialValue = member.name,
            label = "Name",
            isSaving = state.isSavingName,
            serverError = state.nameError,
            validate = { if (it.isBlank()) "Name can't be empty" else null },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onDismiss = { viewModel.onAction(ProfileAction.OnDismissEditName) },
            onConfirm = { viewModel.onAction(ProfileAction.OnSaveName(it)) },
        )
    }
    if (state.showRemoveAvatarConfirmation) {
        ConfirmDialog(
            title = "Remove photo?",
            message = "Your initials will be shown instead.",
            confirmLabel = "Remove",
            onDismiss = { viewModel.onAction(ProfileAction.OnCancelRemoveAvatar) },
            onConfirm = { viewModel.onAction(ProfileAction.OnConfirmRemoveAvatar) },
        )
    }
    if (state.showChangePassword) {
        ChangePasswordDialog(
            isWorking = state.isChangingPassword,
            serverError = state.passwordError,
            onDismiss = { viewModel.onAction(ProfileAction.OnDismissChangePassword) },
            onSubmit = { c, n, r -> viewModel.onAction(ProfileAction.OnSubmitPassword(c, n, r)) },
        )
    }
}

@Composable
private fun ProfileContent(
    member: Member,
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onPickPhoto: () -> Unit,
    onSignOut: () -> Unit,
    isSigningOut: Boolean,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.clickable(enabled = !state.isUploadingAvatar, onClick = onPickPhoto),
            ) {
                MemberAvatar(member.name, member.imageUrl, size = 104.dp)
                if (state.isUploadingAvatar) {
                    Box(
                        Modifier.size(104.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp) }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PhotoCamera, "Change photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(member.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                member.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }

        SettingsSectionHeader("Profile")
        SettingsGroup {
            SettingsRow(
                title = "Display name",
                icon = Icons.Default.Badge,
                value = member.name,
                showChevron = true,
                onClick = { onAction(ProfileAction.OnEditName) },
            )
            SettingsDivider()
            SettingsRow(
                title = if (member.imageUrl == null) "Add photo" else "Change photo",
                icon = Icons.Default.PhotoCamera,
                enabled = !state.isUploadingAvatar,
                showChevron = true,
                onClick = onPickPhoto,
            )
            if (member.imageUrl != null) {
                SettingsDivider()
                SettingsRow(
                    title = "Remove photo",
                    icon = Icons.Default.Delete,
                    destructive = true,
                    enabled = !state.isUploadingAvatar,
                    onClick = { onAction(ProfileAction.OnRemoveAvatar) },
                )
            }
        }

        SettingsSectionHeader("Security")
        SettingsGroup {
            SettingsRow(
                title = "Change password",
                icon = Icons.Default.Lock,
                showChevron = true,
                onClick = { onAction(ProfileAction.OnChangePassword) },
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingsGroup {
            SettingsRow(
                title = if (isSigningOut) "Signing out…" else "Sign out",
                icon = Icons.AutoMirrored.Filled.Logout,
                destructive = true,
                enabled = !isSigningOut,
                onClick = onSignOut,
            )
        }
        Spacer(Modifier.height(32.dp).navigationBarsPadding())
    }
}

@Composable
private fun ChangePasswordDialog(
    isWorking: Boolean,
    serverError: String?,
    onDismiss: () -> Unit,
    onSubmit: (current: String, new: String, revokeOthers: Boolean) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var revokeOthers by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val validation = PasswordRules.validate(current, new, confirm)
    val shownError = (if (attempted) validation else null) ?: serverError

    @Composable
    fun field(value: String, label: String, onChange: (String) -> Unit) = OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = !isWorking,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                field(current, "Current password") { current = it }
                field(new, "New password") { new = it }
                field(confirm, "Confirm new password") { confirm = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = revokeOthers, onCheckedChange = { revokeOthers = it }, enabled = !isWorking)
                    Text("Sign out other devices", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (visible) "Hide passwords" else "Show passwords",
                        )
                    }
                }
                if (shownError != null) {
                    Text(shownError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "At least ${PasswordRules.MIN_LENGTH} characters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    attempted = true
                    if (validation == null) onSubmit(current, new, revokeOthers)
                },
                enabled = !isWorking,
            ) {
                if (isWorking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Update", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") } },
    )
}
