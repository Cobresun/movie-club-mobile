package cobresun.movieclub.app.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.reviews.presentation.ReviewsScreenRoot
import cobresun.movieclub.app.reviews.presentation.ReviewsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

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
            startDestination = Route.ClubGraph,
        ) {
            navigation<Route.ClubGraph>(
                startDestination = Route.ReviewsList()
            ) {
                composable<Route.ReviewsList> {
                    val reviewsViewModel = koinViewModel<ReviewsViewModel>()

                    ReviewsScreenRoot(
                        viewModel = reviewsViewModel
                    )
                }
            }
        }
    }
}
