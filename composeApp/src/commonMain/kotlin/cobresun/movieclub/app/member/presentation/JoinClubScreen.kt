package cobresun.movieclub.app.member.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.compose.koinInject
import cobresun.movieclub.app.core.platform.HapticFeedback

@Composable
fun JoinClubScreenRoot(
    /** Pre-fills the invite (e.g. from a join-club link). */
    token: String?,
    viewModel: JoinClubViewModel = koinViewModel { parametersOf(token) },
    onNavigateBack: () -> Unit,
    onJoined: (clubSlug: String) -> Unit,
    memberClubSlugs: Set<String> = emptySet(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = koinInject<HapticFeedback>()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinClubEvent.Joined -> {
                    haptics.performSuccess()
                    onJoined(event.clubSlug)
                }
            }
        }
    }
    // Like the web app: an invite to a club you're already in just takes you there.
    val alreadyMember = state.invite?.slug?.let { it in memberClubSlugs } == true
    // Web JoinClubView redirects straight to a club you're already in.
    LaunchedEffect(alreadyMember, state.invite?.slug) {
        val slug = state.invite?.slug
        if (alreadyMember && slug != null) onJoined(slug)
    }
    JoinClubScreen(
        state = state,
        onAction = { action ->
            val invite = state.invite
            if (action == JoinClubAction.OnJoin && alreadyMember && invite != null) onJoined(invite.slug)
            else viewModel.onAction(action)
        },
        onNavigateBack = onNavigateBack,
        alreadyMember = alreadyMember,
    )
}

@Composable
fun JoinClubScreen(
    state: JoinClubState,
    onAction: (JoinClubAction) -> Unit,
    onNavigateBack: () -> Unit,
    alreadyMember: Boolean = false,
) {
    Scaffold(
        topBar = { SettingsTopBar("Join a club", onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Got an invite?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            Text(
                "Paste the invite link a club member shared with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = { onAction(JoinClubAction.OnInputChanged(it)) },
                placeholder = { Text("movie-club.app/join-club/…") },
                singleLine = true,
                enabled = !state.isJoining,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = when {
                    state.isLookingUp -> { { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) } }
                    state.input.isNotEmpty() -> {
                        {
                            IconButton(onClick = { onAction(JoinClubAction.OnInputChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                    else -> null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(visible = state.invite != null) {
                val invite = state.invite
                if (invite != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (alreadyMember) "You're already a member of" else "You're invited to join",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Text(invite.clubName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { onAction(JoinClubAction.OnJoin) },
                                enabled = !state.isJoining,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    if (state.isJoining) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            if (alreadyMember) "Open ${invite.clubName}" else "Join ${invite.clubName}",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
