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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ThemeMode
import cobresun.movieclub.app.core.presentation.AppearanceViewModel
import cobresun.movieclub.app.core.presentation.components.ErrorMessage
import cobresun.movieclub.app.core.presentation.components.MemberAvatar
import cobresun.movieclub.app.core.presentation.components.SettingsDivider
import cobresun.movieclub.app.core.presentation.components.SettingsGroup
import cobresun.movieclub.app.core.presentation.components.SettingsRow
import cobresun.movieclub.app.core.presentation.components.SettingsSectionHeader
import cobresun.movieclub.app.core.presentation.components.SettingsTopBar
import cobresun.movieclub.app.core.presentation.platform.rememberImagePicker
import cobresun.movieclub.app.member.domain.Member
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.viewmodel.koinViewModel
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.presentation.toUserMessage

@Composable
fun ProfileScreenRoot(
    viewModel: ProfileViewModel = koinViewModel(),
    appearanceViewModel: AppearanceViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    isSigningOut: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by appearanceViewModel.themeMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(ProfileAction.OnClearMessage)
        }
    }
    // A picked photo goes through the crop step before it's uploaded (web PhotoCropModal).
    var photoToCrop by remember { mutableStateOf<ImageBitmap?>(null) }
    val pickImage = rememberImagePicker { picked ->
        val bytes = picked?.bytes ?: return@rememberImagePicker
        photoToCrop = runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        if (photoToCrop == null) {
            viewModel.onAction(ProfileAction.OnPhotoFailed("That photo couldn't be opened. Try a JPEG or PNG."))
        }
    }

    Scaffold(
        topBar = { SettingsTopBar("Profile", onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when (val member = state.member) {
                is AsyncResult.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is AsyncResult.Error -> ErrorMessage(
                    message = (member.dataError as? DataError.Remote)?.toUserMessage("load your profile")
                        ?: "Couldn't load your profile.",
                    onRetry = { viewModel.onAction(ProfileAction.Retry) },
                    modifier = Modifier.align(Alignment.Center),
                )
                is AsyncResult.Success -> ProfileContent(
                    member = member.data,
                    state = state,
                    onAction = viewModel::onAction,
                    onPickPhoto = pickImage,
                    themeMode = themeMode,
                    onThemeMode = appearanceViewModel::setThemeMode,
                    onSignOut = onSignOut,
                    isSigningOut = isSigningOut,
                )
            }
        }
    }

    photoToCrop?.let { image ->
        PhotoCropSheet(
            image = image,
            onCancel = { photoToCrop = null },
            onSave = { jpeg ->
                photoToCrop = null
                if (jpeg != null) viewModel.onAction(ProfileAction.OnAvatarPicked(jpeg, "image/jpeg"))
                else viewModel.onAction(ProfileAction.OnPhotoFailed("Couldn't prepare that photo"))
            },
        )
    }
}

@Composable
private fun ProfileContent(
    member: Member,
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onPickPhoto: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    isSigningOut: Boolean,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfilePhoto(member, state.isUploadingAvatar, onPickPhoto, onRemove = { onAction(ProfileAction.OnRemoveAvatar) })
            Spacer(Modifier.height(12.dp))
            Text(
                member.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }

        SettingsSectionHeader("Name")
        NameForm(member.name, state, onAction)

        SettingsSectionHeader("Password")
        PasswordForm(state, onAction)

        SettingsSectionHeader("Appearance")
        AppearanceGroup(themeMode, onThemeMode)

        Spacer(Modifier.height(24.dp))
        SettingsGroup {
            SettingsRow(
                title = if (isSigningOut) "Logging out…" else "Log out",
                icon = Icons.AutoMirrored.Filled.Logout,
                destructive = true,
                enabled = !isSigningOut,
                onClick = onSignOut,
            )
        }
        Spacer(Modifier.height(32.dp).navigationBarsPadding())
    }
}

/** Dark (the default, like the web app), Light, or following the system setting. */
@Composable
private fun AppearanceGroup(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SettingsGroup(Modifier.selectableGroup()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            if (index > 0) SettingsDivider()
            val isSelected = mode == selected
            SettingsRow(
                title = when (mode) {
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.SYSTEM -> "Match system"
                },
                icon = when (mode) {
                    ThemeMode.DARK -> Icons.Outlined.DarkMode
                    ThemeMode.LIGHT -> Icons.Outlined.LightMode
                    ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                },
                modifier = Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mode) }),
                trailing = {
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                },
            )
        }
    }
}

/**
 * The avatar is the control (web ProfileView): a pencil badge says it can be
 * changed, a red badge removes the photo, and a scrim spins while it saves.
 */
@Composable
private fun ProfilePhoto(member: Member, isPending: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    val ring = MaterialTheme.colorScheme.surface
    Box {
        Box(
            Modifier
                .clip(CircleShape)
                .clickable(enabled = !isPending, onClickLabel = "Change photo", role = Role.Button, onClick = onPick),
        ) {
            MemberAvatar(member.name, member.imageUrl, size = 120.dp)
            if (isPending) {
                Box(
                    Modifier.size(120.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                        .semantics { contentDescription = "Updating photo" },
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp) }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .border(2.dp, ring, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        if (member.imageUrl != null && !isPending) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .border(2.dp, ring, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClickLabel = "Remove photo", role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = "Remove photo" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun NameForm(currentName: String, state: ProfileState, onAction: (ProfileAction) -> Unit) {
    // Follows the name the server last gave us (every save reloads it).
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(MAX_NAME_LENGTH)
                onAction(ProfileAction.OnNameEdited)
            },
            label = { Text("Your name") },
            placeholder = { Text("Enter your name") },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = { Text(state.nameError ?: "Shown to everyone in your clubs") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(ProfileAction.OnSaveName(name)) }),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onAction(ProfileAction.OnSaveName(name)) },
            enabled = !state.isSavingName,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Save name", fontWeight = FontWeight.SemiBold) }
    }
}

/** Web ChangePasswordForm, inline on the profile. */
@Composable
private fun PasswordForm(state: ProfileState, onAction: (ProfileAction) -> Unit) {
    var current by rememberSaveable(state.passwordChanges) { mutableStateOf("") }
    var new by rememberSaveable(state.passwordChanges) { mutableStateOf("") }
    var revokeOthers by rememberSaveable { mutableStateOf(true) }
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.passwordError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)).padding(12.dp),
            )
        }
        OutlinedTextField(
            value = current,
            onValueChange = { current = it; onAction(ProfileAction.OnPasswordEdited) },
            label = { Text("Current password") },
            placeholder = { Text("Enter current password") },
            singleLine = true,
            enabled = !state.isChangingPassword,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = new,
            onValueChange = { new = it; onAction(ProfileAction.OnPasswordEdited) },
            label = { Text("New password") },
            placeholder = { Text("At least ${PasswordRules.MIN_LENGTH} characters") },
            supportingText = { Text("At least ${PasswordRules.MIN_LENGTH} characters.") },
            singleLine = true,
            enabled = !state.isChangingPassword,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Switch) { revokeOthers = !revokeOthers }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sign out of all other devices", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Ends every session except this one",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Switch(checked = revokeOthers, onCheckedChange = { revokeOthers = it })
        }
        Button(
            onClick = { onAction(ProfileAction.OnSubmitPassword(current, new, revokeOthers)) },
            enabled = !state.isChangingPassword,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (state.isChangingPassword) "Updating…" else "Update password", fontWeight = FontWeight.SemiBold) }
    }
}
