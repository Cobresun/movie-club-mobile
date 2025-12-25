package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.member.domain.Member
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubSettingsScreenRoot(
    viewModel: ClubSettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateAfterLeave: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ClubSettingsAction.OnClearError)
        }
    }

    // Show success messages
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ClubSettingsAction.OnClearSuccess)
        }
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ClubSettingsNavigationEvent.NavigateAfterLeave -> {
                    onNavigateAfterLeave()
                }
            }
        }
    }

    ClubSettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubSettingsScreen(
    state: ClubSettingsState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAction: (ClubSettingsAction) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Club Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Members Section
            MembersSection(members = state.members)

            // Invite Link Section
            InviteLinkSection(
                inviteLink = state.inviteLink,
                hasCopiedLink = state.hasCopiedLink,
                onCopyClick = { onAction(ClubSettingsAction.OnCopyInviteLink) }
            )

            // Leave Club Section
            LeaveClubSection(
                onLeaveClick = { onAction(ClubSettingsAction.OnLeaveClub) },
                isLeavingClub = state.isLeavingClub
            )
        }

        // Leave Confirmation Dialog
        if (state.showLeaveConfirmation) {
            AlertDialog(
                onDismissRequest = { onAction(ClubSettingsAction.OnCancelLeaveClub) },
                title = { Text("Leave Club?") },
                text = {
                    Text("Are you sure you want to leave this club? You will need an invite link to rejoin.")
                },
                confirmButton = {
                    TextButton(
                        onClick = { onAction(ClubSettingsAction.OnConfirmLeaveClub) }
                    ) {
                        Text("Leave", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(ClubSettingsAction.OnCancelLeaveClub) }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MembersSection(members: AsyncResult<List<Member>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Members",
            style = MaterialTheme.typography.titleLarge
        )

        AsyncResultHandler(members) { memberList ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                memberList.forEach { member ->
                    MemberCard(member = member)
                }
            }
        }
    }
}

@Composable
private fun MemberCard(member: Member) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = member.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InviteLinkSection(
    inviteLink: String,
    hasCopiedLink: Boolean,
    onCopyClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Invite Link",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inviteLink,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = onCopyClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (hasCopiedLink) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.ContentCopy
                            },
                            contentDescription = if (hasCopiedLink) "Copied" else "Copy",
                            tint = if (hasCopiedLink) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Text(
                    text = "Share this link to invite people to your club",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeaveClubSection(
    onLeaveClick: () -> Unit,
    isLeavingClub: Boolean = false
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLeaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLeavingClub,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            if (isLeavingClub) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leaving...")
            } else {
                Text("Leave Club")
            }
        }
    }
}
