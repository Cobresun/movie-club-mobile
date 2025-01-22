package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.app.Route
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.presentation.MemberViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubsScreenRoot(
    viewModel: MemberViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClubsScreen(clubs = state.clubs)
}

@Composable
fun ClubsScreen(
    clubs: AsyncResult<List<Club>>,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerContent(
                clubs = clubs,
                onClubClick = {
                    navController.navigate(Route.Club(it))
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
            }
        }
    }
}

@Composable
private fun EmptyClubsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = "No clubs")
    }
}

@Composable
private fun ModalDrawerContent(
    clubs: AsyncResult<List<Club>>,
    onClubClick: (String) -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.safeContentPadding()
        ) {
            when (clubs) {
                is AsyncResult.Error -> {}
                is AsyncResult.Loading -> {}
                is AsyncResult.Success -> {
                    AsyncResultHandler(clubs) {
                        it.forEach { club ->
                            Text(
                                text = club.name,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable { onClubClick(club.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
