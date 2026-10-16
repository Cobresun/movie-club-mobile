package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import cobresun.movieclub.app.member.presentation.components.ConfirmDialog
import cobresun.movieclub.app.member.presentation.components.ErrorMessage
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.SettingsDivider
import cobresun.movieclub.app.member.presentation.components.SettingsFooter
import cobresun.movieclub.app.member.presentation.components.SettingsGroup
import cobresun.movieclub.app.member.presentation.components.SettingsRow
import cobresun.movieclub.app.member.presentation.components.SettingsSectionHeader
import cobresun.movieclub.app.member.presentation.components.SettingsSwitchRow
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import cobresun.movieclub.app.member.presentation.components.TextInputDialog
import cobresun.movieclub.app.member.presentation.components.icon
import cobresun.movieclub.app.member.presentation.components.label
import cobresun.movieclub.app.member.presentation.platform.rememberShareSheet
import org.koin.compose.viewmodel.koinViewModel

private const val SLUG_PREFIX = "movie-club.app/club/"

@Composable
fun ClubSettingsScreenRoot(
    viewModel: ClubSettingsViewModel = koinViewModel(),
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
                item { ClubHeader(club.data) }
                generalSection(club.data, onAction)
                featuresSection(club.data, state, onAction)
                inviteSection(state, onAction, onShare = { link -> share(link, "Join ${club.data.name} on Movie Club") })
                membersSection(state, onAction)
                item {
                    Spacer(Modifier.height(24.dp))
                    SettingsGroup {
                        SettingsRow(
                            title = if (state.isLeavingClub) "Leaving…" else "Leave club",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            destructive = true,
                            enabled = !state.isLeavingClub,
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
        EditField.NAME -> TextInputDialog(
            title = "Rename club",
            initialValue = loadedClub?.name.orEmpty(),
            label = "Club name",
            isSaving = state.isSaving,
            serverError = state.editError,
            validate = ClubValidation::validateName,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            onDismiss = { onAction(ClubSettingsAction.OnDismissEdit) },
            onConfirm = { onAction(ClubSettingsAction.OnSaveName(it)) },
        )
        EditField.SLUG -> TextInputDialog(
            title = "Change club URL",
            initialValue = loadedClub?.slug.orEmpty(),
            label = "URL",
            prefix = SLUG_PREFIX,
            supportingText = "Old links will stop working.",
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
        ConfirmDialog(
            title = "Remove ${member.name}?",
            message = "They'll lose access to this club. You can invite them back later.",
            confirmLabel = "Remove",
            isWorking = state.isRemovingMember,
            onDismiss = { onAction(ClubSettingsAction.OnCancelRemoveMember) },
            onConfirm = { onAction(ClubSettingsAction.OnConfirmRemoveMember) },
        )
    }
    if (state.showLeaveConfirmation) {
        ConfirmDialog(
            title = "Leave ${loadedClub?.name ?: "club"}?",
            message = "You'll need a new invite link to rejoin.",
            confirmLabel = "Leave",
            isWorking = state.isLeavingClub,
            onDismiss = { onAction(ClubSettingsAction.OnCancelLeaveClub) },
            onConfirm = { onAction(ClubSettingsAction.OnConfirmLeaveClub) },
        )
    }
}

@Composable
private fun ClubHeader(club: Club) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(club.type.icon(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            club.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            club.type.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

private fun LazyListScope.generalSection(club: Club, onAction: (ClubSettingsAction) -> Unit) {
    item {
        SettingsSectionHeader("General")
        SettingsGroup {
            SettingsRow(
                title = "Name",
                icon = Icons.Default.Title,
                value = club.name,
                showChevron = true,
                onClick = { onAction(ClubSettingsAction.OnEditName) },
            )
            SettingsDivider()
            SettingsRow(
                title = "URL",
                icon = Icons.Default.Link,
                subtitle = SLUG_PREFIX + club.slug,
                showChevron = true,
                onClick = { onAction(ClubSettingsAction.OnEditSlug) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Type",
                icon = club.type.icon(),
                value = if (club.type == ClubType.MOVIE) "Movies" else "Books",
            )
        }
    }
}

private fun LazyListScope.featuresSection(
    club: Club,
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
) {
    item {
        SettingsSectionHeader("Features")
        val features = state.features
        SettingsGroup {
            if (club.type == ClubType.MOVIE) {
                SettingsSwitchRow(
                    title = "Awards",
                    subtitle = "Yearly nominations and ranked voting",
                    icon = Icons.Default.EmojiEvents,
                    checked = features?.awards == true,
                    enabled = features != null,
                    onCheckedChange = { onAction(ClubSettingsAction.OnToggleAwards(it)) },
                )
                SettingsDivider()
            }
            SettingsSwitchRow(
                title = "Discussion questions",
                subtitle = "Prompts to get conversation going",
                icon = Icons.Default.Forum,
                checked = features?.discussionQuestions == true,
                enabled = features != null,
                onCheckedChange = { onAction(ClubSettingsAction.OnToggleDiscussionQuestions(it)) },
            )
        }
    }
}

private fun LazyListScope.inviteSection(
    state: ClubSettingsState,
    onAction: (ClubSettingsAction) -> Unit,
    onShare: (String) -> Unit,
) {
    item {
        SettingsSectionHeader("Invite")
        SettingsGroup {
            val link = state.inviteLink
            if (link == null) {
                SettingsRow(
                    title = if (state.isCreatingInvite) "Creating link…" else "Create invite link",
                    icon = Icons.Default.PersonAdd,
                    enabled = !state.isCreatingInvite,
                    trailing = if (state.isCreatingInvite) {
                        { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
                    } else null,
                    onClick = { onAction(ClubSettingsAction.OnCreateInviteLink) },
                )
            } else {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        link,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = { onAction(ClubSettingsAction.OnCopyInviteLink) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = { onShare(link) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.IosShare, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
        SettingsFooter("Anyone with the link can join. Links expire after a while.")
    }
}

private fun LazyListScope.membersSection(state: ClubSettingsState, onAction: (ClubSettingsAction) -> Unit) {
    val members = state.members
    item {
        val count = (members as? AsyncResult.Success)?.data?.size
        SettingsSectionHeader(if (count != null) "Members · $count" else "Members")
    }
    when (members) {
        is AsyncResult.Loading -> item {
            SettingsGroup {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        is AsyncResult.Error -> item {
            SettingsGroup {
                SettingsRow(
                    title = "Couldn't load members",
                    subtitle = "Tap to try again",
                    onClick = { onAction(ClubSettingsAction.Retry) },
                )
            }
        }
        is AsyncResult.Success -> item {
            val sorted = members.data.sortedWith(
                compareBy<Member>({ it.id != state.currentUserId }, { it.role != MemberRole.ADMIN }, { it.name.lowercase() })
            )
            SettingsGroup {
                sorted.forEachIndexed { index, member ->
                    if (index > 0) SettingsDivider(inset = 66.dp)
                    MemberRow(
                        member = member,
                        isSelf = member.id == state.currentUserId,
                        onRemove = { onAction(ClubSettingsAction.OnRemoveMember(member)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: Member, isSelf: Boolean, onRemove: () -> Unit) {
    SettingsRow(
        title = member.name.ifBlank { member.email },
        subtitle = member.email,
        leading = { MemberAvatar(member.name, member.imageUrl, size = 40.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isSelf) Badge("You", MaterialTheme.colorScheme.tertiary)
                if (member.role == MemberRole.ADMIN) Badge("Admin")
                if (!isSelf) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.PersonRemove,
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
