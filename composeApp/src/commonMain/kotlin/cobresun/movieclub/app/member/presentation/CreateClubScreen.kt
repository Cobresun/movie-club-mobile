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
import cobresun.movieclub.app.member.presentation.components.SettingsSectionHeader
import cobresun.movieclub.app.member.presentation.components.SettingsTopBar
import cobresun.movieclub.app.member.presentation.components.icon
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateClubScreenRoot(
    viewModel: CreateClubViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToClub: (slug: String, name: String, type: ClubType) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is CreateClubNavigationEvent.NavigateToClub -> onNavigateToClub(event.clubSlug, event.clubName, event.type)
            }
        }
    }

    CreateClubScreen(state = state, onNavigateBack = onNavigateBack, onAction = viewModel::onAction)
}

@Composable
fun CreateClubScreen(
    state: CreateClubState,
    onNavigateBack: () -> Unit,
    onAction: (CreateClubAction) -> Unit
) {
    Scaffold(
        topBar = { SettingsTopBar("New club", onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Start a club",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Text(
                "Pick a name and what you'll be reviewing. You can invite friends once it's created.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            SettingsSectionHeader("Name")
            OutlinedTextField(
                value = state.clubName,
                onValueChange = { onAction(CreateClubAction.OnClubNameChanged(it)) },
                placeholder = { Text("e.g. Friday Night Flicks") },
                enabled = !state.isCreating,
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAction(CreateClubAction.OnCreateClub) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            SettingsSectionHeader("Type")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onAction(CreateClubAction.OnCreateClub) },
                enabled = !state.isCreating && state.clubName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 52.dp)
                    .navigationBarsPadding(),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Create club", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
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
