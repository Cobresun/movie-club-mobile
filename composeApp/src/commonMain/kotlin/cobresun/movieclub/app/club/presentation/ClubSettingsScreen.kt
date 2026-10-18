package cobresun.movieclub.app.club.presentation

import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.ConfirmSheet
import cobresun.movieclub.app.member.presentation.components.SettingsIcon
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.club.domain.ClubValidation
import cobresun.movieclub.app.club.domain.MemberRole
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.presentation.components.Badge
import cobresun.movieclub.app.member.presentation.components.ErrorMessage
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.SettingsDivider
import cobresun.movieclub.app.member.presentation.components.SettingsGroup
import cobresun.movieclub.app.member.presentation.components.SettingsRow
import cobresun.movieclub.app.member.presentation.components.SettingsSectionHeader
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import cobresun.movieclub.app.core.presentation.components.TextInputSheet
import cobresun.movieclub.app.member.presentation.components.icon
import cobresun.movieclub.app.member.presentation.components.label
import cobresun.movieclub.app.member.presentation.platform.rememberShareSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SLUG_PREFIX = "movie-club.app/club/"

@Composable
fun ClubSettingsScreenRoot(
    clubSlug: String,
    viewModel: ClubSettingsViewModel = koinViewModel { parametersOf(clubSlug) },
    onNavigateBack: () -> Unit,
    onNavigateAfterLeave: () -> Unit,
    onClubUpdated: (newSlug: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                ClubSettingsNavigationEvent.NavigateAfterLeave -> onNavigateAfterLeave()
                is ClubSettingsNavigationEvent.ClubUpdated -> onClubUpdated(event.newSlug)
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(ClubSettingsAction.OnClearMessage)
        }
    }

    Scaffold(
        topBar = { SettingsTopBar("Club settings", onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        ClubSettingsScreen(
            state = state,
            onAction = viewModel::onAction,
            contentPadding = padding,
        )
    }
}

@Composable
fun ClubSettingsScreen(
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
    contentPadding: PaddingValues,
) {
    val share = rememberShareSheet()
    val haptics = koinInject<HapticFeedback>()
    val club = state.club

    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        when (club) {
            is AsyncResult.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is AsyncResult.Error -> ErrorMessage(
                message = club.exception?.message ?: "Couldn't load club settings.",
                onRetry = { onAction(ClubSettingsAction.Retry) },
                modifier = Modifier.align(Alignment.Center),
            )
            is AsyncResult.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item { ClubHeader(club.data, (state.members as? AsyncResult.Success)?.data?.size) }
                membersSection(
                    state = state,
                    onAction = onAction,
                    onCopyInvite = {
                        haptics.performConfirm()
                        onAction(ClubSettingsAction.OnCopyInviteLink)
                    },
                    onShareInvite = { link -> share("${club.data.type.inviteShareText()}\n$link", "Join ${club.data.name}") },
                )
                settingsSection(club.data, state, onAction)
                item {
                    Spacer(Modifier.height(24.dp))
                    SettingsGroup {
                        SettingsRow(
                            title = "Leave club",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            destructive = true,
                            showChevron = true,
                            onClick = { onAction(ClubSettingsAction.OnLeaveClub) },
                        )
                    }
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }

    val loadedClub = (club as? AsyncResult.Success)?.data
    when (state.editing) {
        EditField.NAME -> TextInputSheet(
            title = "Club name",
            confirmLabel = "Save",
            requireChange = true,
            dismissOnConfirm = false,
            initialValue = loadedClub?.name.orEmpty(),
            label = "Club name",
            isSaving = state.isSaving,
            serverError = state.editError,
            validate = ClubValidation::validateName,
            transform = { it.take(ClubValidation.MAX_NAME_LENGTH) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onDismiss = { onAction(ClubSettingsAction.OnDismissEdit) },
            onConfirm = { onAction(ClubSettingsAction.OnSaveName(it.trim())) },
        )
        EditField.SLUG -> TextInputSheet(
            title = "Club link",
            confirmLabel = "Save",
            requireChange = true,
            dismissOnConfirm = false,
            initialValue = loadedClub?.slug.orEmpty(),
            label = "Club link",
            prefix = SLUG_PREFIX,
            supportingText = "Changing this breaks existing links to your club.",
            isSaving = state.isSaving,
            serverError = state.editError,
            validate = { ClubValidation.validateSlug(it) },
            transform = ClubValidation::sanitizeSlugInput,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
            onDismiss = { onAction(ClubSettingsAction.OnDismissEdit) },
            onConfirm = { onAction(ClubSettingsAction.OnSaveSlug(it)) },
        )
        null -> Unit
    }

    state.memberPendingRemoval?.let { member ->
        ConfirmSheet(
            title = "Remove ${member.name}?",
            message = "They lose access to this club. Their reviews and scores stay.",
            confirmLabel = "Remove",
            destructive = true,
            onDismiss = { onAction(ClubSettingsAction.OnCancelRemoveMember) },
            onConfirm = { onAction(ClubSettingsAction.OnConfirmRemoveMember) },
        )
    }
    if (state.showLeaveConfirmation) {
        ConfirmSheet(
            title = "Leave club?",
            message = "Are you sure you want to leave this club? This action cannot be undone.",
            confirmLabel = "Leave",
            destructive = true,
            loading = state.isLeavingClub,
            dismissOnConfirm = false,
            onDismiss = { onAction(ClubSettingsAction.OnCancelLeaveClub) },
            onConfirm = { onAction(ClubSettingsAction.OnConfirmLeaveClub) },
        )
    }
}

@Composable
private fun ClubHeader(club: Club, memberCount: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MemberAvatar(club.name, null, size = 56.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                club.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                clubMetaLine(club.type, memberCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.membersSection(
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
    onCopyInvite: () -> Unit,
    onShareInvite: (String) -> Unit,
) {
    val members = state.members
    item {
        val count = (members as? AsyncResult.Success)?.data?.size
        Row(verticalAlignment = Alignment.Bottom) {
            SettingsSectionHeader("Members", Modifier.weight(1f))
            if (count != null && count > 0) {
                Text(
                    "$count ${if (count == 1) "person" else "people"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 20.dp, bottom = 8.dp),
                )
            }
        }
    }
    item {
        SettingsGroup {
            when (members) {
                is AsyncResult.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                is AsyncResult.Error -> SettingsRow(
                    title = "Couldn't load members",
                    subtitle = "Tap to try again",
                    onClick = { onAction(ClubSettingsAction.Retry) },
                )
                is AsyncResult.Success -> members.data.forEachIndexed { index, member ->
                    if (index > 0) SettingsDivider(inset = 62.dp)
                    MemberRow(
                        member = member,
                        isSelf = member.id == state.currentUserId,
                        onRemove = { onAction(ClubSettingsAction.OnRemoveMember(member)) },
                    )
                }
            }
            SettingsDivider(inset = 0.dp)
            InviteRow(state, onAction, onCopyInvite, onShareInvite)
        }
    }
}

@Composable
private fun InviteRow(
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
    onCopy: () -> Unit,
    onShare: (String) -> Unit,
) {
    val link = state.inviteLink
    SettingsRow(
        title = "Invite people",
        subtitle = when {
            link != null -> "Copy the club link"
            state.isCreatingInvite -> "Getting the club link…"
            else -> "Couldn't get the link. Tap to try again"
        },
        icon = Icons.Default.Link,
        onClick = if (link != null) onCopy else ({ onAction(ClubSettingsAction.OnCreateInviteLink) }),
        enabled = !state.isCreatingInvite,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isCreatingInvite) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else if (link != null) {
                    AnimatedContent(state.inviteCopied, label = "invite-copied") { copied ->
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (copied) "Invite link copied" else "Copy invite link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { onShare(link) }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share invite link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    )
}

private fun LazyListScope.settingsSection(
    club: Club,
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
) {
    item {
        SettingsSectionHeader("Club settings")
        val features = state.features
        SettingsGroup {
            SettingsRow(
                title = "Club name",
                icon = Icons.Default.Title,
                subtitle = club.name,
                trailing = { EditGlyph() },
                onClick = { onAction(ClubSettingsAction.OnEditName) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Club link",
                icon = Icons.Default.Link,
                subtitle = SLUG_PREFIX + club.slug,
                trailing = { EditGlyph() },
                onClick = { onAction(ClubSettingsAction.OnEditSlug) },
            )
            // Web clubSections: awards are a movie-club feature.
            if (club.type == ClubType.MOVIE) {
                SettingsDivider()
                FeatureRow(
                    title = "Awards",
                    description = "Season awards, ballots and winners for this club.",
                    icon = Icons.Default.EmojiEvents,
                    checked = features?.awards == true,
                    enabled = features != null,
                    onCheckedChange = { onAction(ClubSettingsAction.OnToggleAwards(it)) },
                )
            }
            SettingsDivider()
            FeatureRow(
                title = "AI discussion questions",
                description = "Adds a button on each review that drafts questions to spark conversation.",
                icon = Icons.Default.Forum,
                checked = features?.discussionQuestions == true,
                enabled = features != null,
                onCheckedChange = { onAction(ClubSettingsAction.OnToggleDiscussionQuestions(it)) },
            )
        }
    }
}

@Composable
private fun EditGlyph() {
    Icon(
        Icons.Default.Edit,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(20.dp),
    )
}

/** A feature switch with its description and the web's "Experimental" warning label. */
@Composable
private fun FeatureRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(13.dp))
                Text("Experimental", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }
}

@Composable
private fun MemberRow(member: Member, isSelf: Boolean, onRemove: () -> Unit) {
    SettingsRow(
        title = member.name.ifBlank { member.email },
        subtitle = member.email,
        leading = { MemberAvatar(member.name, member.imageUrl, size = 36.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSelf) Badge("You", MaterialTheme.colorScheme.tertiary)
                // Web rule: admins can't be removed, so they show the badge instead of the button.
                if (member.role == MemberRole.ADMIN) {
                    Badge("Admin")
                } else if (!isSelf) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove ${member.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    )
}
