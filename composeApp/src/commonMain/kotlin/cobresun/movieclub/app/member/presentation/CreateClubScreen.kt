package cobresun.movieclub.app.member.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateClubScreenRoot(
    viewModel: CreateClubViewModel = koinViewModel(),
    memberViewModel: MemberViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToClub: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is CreateClubNavigationEvent.NavigateToClub -> {
                    memberViewModel.refresh()
                    onNavigateToClub(event.clubId)
                }
            }
        }
    }

    // Show error message
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show Snackbar when available
            viewModel.onAction(CreateClubAction.OnClearError)
        }
    }

    CreateClubScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(
    state: CreateClubState,
    onNavigateBack: () -> Unit,
    onAction: (CreateClubAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Club") },
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.clubName,
                onValueChange = { onAction(CreateClubAction.OnClubNameChanged(it)) },
                label = { Text("Club Name") },
                enabled = !state.isCreating,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onAction(CreateClubAction.OnCreateClub) },
                enabled = !state.isCreating && state.clubName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Text("Create Club")
                }
            }
        }
    }
}
