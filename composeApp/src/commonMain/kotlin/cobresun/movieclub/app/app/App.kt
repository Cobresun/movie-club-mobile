package cobresun.movieclub.app.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cobresun.movieclub.app.reviews.presentation.ReviewsScreenRoot
import cobresun.movieclub.app.watchlist.presentation.WatchListScreenRoot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            surface = Color(0xFF222831)
        )
    ) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Route.ClubGraph(),
        ) {
            composable<Route.ClubGraph> { nav: NavBackStackEntry ->
                val clubId = nav.toRoute<Route.ClubGraph>().clubId
                ClubScreen(clubId)
            }
        }
    }
}

@Composable
fun ClubScreen(clubId: String) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            navController.navigate(Route.ReviewsList(clubId))
        } else {
            navController.navigate(Route.WatchList(clubId))
        }
    }

    Column(modifier = Modifier.statusBarsPadding()) {
        NavHost(
            navController = navController,
            startDestination = Route.ReviewsList(clubId),
            modifier = Modifier.weight(1f)
        ) {
            composable<Route.ReviewsList> {
                ReviewsScreenRoot()
            }

            composable<Route.WatchList> {
                WatchListScreenRoot()
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.navigationBarsPadding(),
            tabs = {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Reviews",
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Watch List",
                        )
                    }
                )
            }
        )
    }
}
