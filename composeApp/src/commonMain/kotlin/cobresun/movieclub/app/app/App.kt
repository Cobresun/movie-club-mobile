package cobresun.movieclub.app.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.reviews.presentation.ReviewsScreenRoot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Route.ReviewsGraph
        ) {
            navigation<Route.ReviewsGraph>(
                startDestination = Route.ReviewsList
            ) {
                composable<Route.ReviewsList> {
                    ReviewsScreenRoot()
                }
            }
        }
    }
}
