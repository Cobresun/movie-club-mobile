package cobresun.movieclub.app.member.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.club.domain.ClubValidation
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import cobresun.movieclub.app.member.presentation.components.icon
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateClubScreenRoot(
    viewModel: CreateClubViewModel = koinViewModel(),
    /** Null when this is the club-less home (nothing to go back to). */
    onNavigateBack: (() -> Unit)?,
    onNavigateToClub: (slug: String, name: String, type: ClubType) -> Unit,
    onJoin: (token: String) -> Unit,
    /** The onboarding framing for someone not in any club yet (web NewClubView). */
    isFirstClub: Boolean = false,
    topEnd: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is CreateClubNavigationEvent.NavigateToClub -> onNavigateToClub(event.clubSlug, event.clubName, event.type)
                is CreateClubNavigationEvent.NavigateToJoin -> onJoin(event.token)
            }
        }
    }

    CreateClubScreen(
        state = state,
        isFirstClub = isFirstClub,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction,
        topEnd = topEnd,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(
    state: CreateClubState,
    isFirstClub: Boolean,
    onNavigateBack: (() -> Unit)?,
    onAction: (CreateClubAction) -> Unit,
    topEnd: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                SettingsTopBar("", onNavigateBack)
            } else {
                Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.padding(end = 8.dp)) { topEnd() }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isFirstClub) "You're not in a club yet" else "New club",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (isFirstClub) {
                        "A club is a handful of friends who watch the same films — or read the same books — and score them together."
                    } else {
                        "Start a second club, or join one you were invited to."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FormCard {
                Text("Start a club", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = state.clubName,
                    onValueChange = { onAction(CreateClubAction.OnClubNameChanged(it.take(ClubValidation.MAX_NAME_LENGTH))) },
                    placeholder = { Text("Club name") },
                    enabled = !state.isCreating,
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAction(CreateClubAction.OnCreateClub) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "What will this club review?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeCard(
                        type = ClubType.MOVIE,
                        title = "Movies",
                        description = "Watchlist, reviews and awards",
                        selected = state.type == ClubType.MOVIE,
                        enabled = !state.isCreating,
                        onClick = { onAction(CreateClubAction.OnTypeChanged(ClubType.MOVIE)) },
                        modifier = Modifier.weight(1f),
                    )
                    TypeCard(
                        type = ClubType.BOOK,
                        title = "Books",
                        description = "Reading list and reviews",
                        selected = state.type == ClubType.BOOK,
                        enabled = !state.isCreating,
                        onClick = { onAction(CreateClubAction.OnTypeChanged(ClubType.BOOK)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(
                    onClick = { onAction(CreateClubAction.OnCreateClub) },
                    enabled = !state.isCreating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Create club", fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    "You'll get an invite link to send your friends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Text("or", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }

            FormCard {
                Text("Join one you were invited to", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Someone already in the club sends you a link. Open it on this device, or paste it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.invite,
                        onValueChange = { onAction(CreateClubAction.OnInviteChanged(it)) },
                        placeholder = { Text("Paste invite link") },
                        singleLine = true,
                        isError = state.inviteError != null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { onAction(CreateClubAction.OnJoin) }),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Invite link" },
                    )
                    FilledTonalButton(
                        onClick = { onAction(CreateClubAction.OnJoin) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) { Text("Join") }
                }
                state.inviteError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FormCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun TypeCard(
    type: ClubType,
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else Color.Transparent),
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(type.icon(), null, tint = accent)
                }
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(22.dp),
                )
            }
        }
    }
}
