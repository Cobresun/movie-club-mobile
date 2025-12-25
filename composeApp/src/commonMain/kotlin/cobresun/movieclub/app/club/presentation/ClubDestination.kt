package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.app.Route
import cobresun.movieclub.app.auth.presentation.AuthAction
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.presentation.CreateClubScreenRoot
import cobresun.movieclub.app.member.presentation.MemberViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubsScreenRoot(
    viewModel: MemberViewModel = koinViewModel(),
    authViewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClubsScreen(
        clubs = state.clubs,
        onLogout = { authViewModel.onAction(AuthAction.Logout) },
        memberViewModel = viewModel
    )
}

@Composable
fun ClubsScreen(
    clubs: AsyncResult<List<Club>>,
    onLogout: () -> Unit = {},
    memberViewModel: MemberViewModel,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Track the current club ID from navigation
    var currentClubId by remember { mutableStateOf<String?>(null) }

    // Update currentClubId when clubs change
    LaunchedEffect(clubs) {
        if (clubs is AsyncResult.Success && clubs.data.isNotEmpty()) {
            val clubIds = clubs.data.map { it.id }

            // If no club selected, select the first one
            if (currentClubId == null) {
                currentClubId = clubs.data.first().id
            }
            // If current club was removed, switch to another club
            else if (currentClubId !in clubIds) {
                val newClubId = clubs.data.first().id
                currentClubId = newClubId

                // Navigate to the new club
                navController.navigate(Route.Club(newClubId)) {
                    // Clear back stack to prevent going back to the removed club
                    popUpTo<Route.Club> { inclusive = true }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerContent(
                clubs = clubs,
                currentClubId = currentClubId,
                onClubClick = { clubId ->
                    currentClubId = clubId
                    navController.navigate(Route.Club(clubId)) {
                        popUpTo<Route.Club> { inclusive = true }
                    }
                    coroutineScope.launch { drawerState.close() }
                },
                onCreateClubClick = {
                    navController.navigate(Route.CreateClub)
                    coroutineScope.launch { drawerState.close() }
                },
                onLogout = onLogout,
                onSettingsClick = { clubId ->
                    navController.navigate(Route.ClubSettings(clubId))
                    coroutineScope.launch { drawerState.close() }
                }
            )
        },
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
        drawerState = drawerState,
    ) {
        AsyncResultHandler(clubs) {
            NavHost(
                navController = navController,
                startDestination = if (it.isEmpty()) {
                    Route.EmptyClubs
                } else {
                    Route.Club(it.first().id)
                },
            ) {
                composable<Route.Club> {
                    ClubScreenRoot()
                }

                composable<Route.EmptyClubs> {
                    EmptyClubsScreen()
                }

                composable<Route.CreateClub> {
                    CreateClubScreenRoot(
                        memberViewModel = memberViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToClub = { clubId ->
                            currentClubId = clubId
                            navController.navigate(Route.Club(clubId)) {
                                popUpTo<Route.CreateClub> { inclusive = true }
                            }
                        }
                    )
                }

                composable<Route.ClubSettings> {
                    ClubSettingsScreenRoot(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateAfterLeave = {
                            // Refresh clubs list
                            memberViewModel.refresh()

                            // Pop both ClubSettings and Club screens
                            // NavHost will observe updated clubs and redirect appropriately
                            navController.popBackStack()
                            navController.popBackStack()

                            // Close drawer
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyClubsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = "No clubs")
    }
}

@Composable
private fun ModalDrawerContent(
    clubs: AsyncResult<List<Club>>,
    currentClubId: String?,
    onClubClick: (String) -> Unit,
    onCreateClubClick: () -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: (String) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            Text(
                text = "Clubs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            when (clubs) {
                is AsyncResult.Error -> {}
                is AsyncResult.Loading -> {}
                is AsyncResult.Success -> {
                    AsyncResultHandler(clubs) {
                        it.forEach { club ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (club.id == currentClubId)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onClubClick(club.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = club.name,
                                    color = if (club.id == currentClubId)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (club.id == currentClubId) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Create Club Button
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onCreateClubClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Create New Club")
                }
            }

            // Push settings and logout buttons to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Club Settings button
            TextButton(
                onClick = {
                    currentClubId?.let { clubId ->
                        onSettingsClick(clubId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Club Settings")
            }

            // Visual separator
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Logout button
            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
        }
    }
}
