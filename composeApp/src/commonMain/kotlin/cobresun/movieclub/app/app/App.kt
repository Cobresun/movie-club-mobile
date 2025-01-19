package cobresun.movieclub.app.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.DrawerValue
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.club.presentation.ClubScreenRoot
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.presentation.MemberViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            surface = Color(0xFF222831),
            onSurface = Color.White,
            primary = Color(0xFF2196F3),
            background = Color(0xFF222831),
            onBackground = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()

            val memberViewModel = koinViewModel<MemberViewModel>()
            val state by memberViewModel.state.collectAsStateWithLifecycle()

            ModalDrawer(
                drawerContent = {
                    ModalDrawerContent(
                        clubs = state.clubs,
                        onClubClick = {
                            navController.navigate(Route.ClubGraph(it))
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                },
                drawerState = drawerState,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Route.ClubGraph(),
                ) {
                    composable<Route.ClubGraph> {
                        ClubScreenRoot()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalDrawerContent(
    clubs: AsyncResult<List<Club>>,
    onClubClick: (String) -> Unit,
) {
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
