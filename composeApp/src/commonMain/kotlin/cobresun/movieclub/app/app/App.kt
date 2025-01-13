package cobresun.movieclub.app.app

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.club.presentation.ClubScreenRoot
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
            composable<Route.ClubGraph> {
                ClubScreenRoot()
            }
        }
    }
}
