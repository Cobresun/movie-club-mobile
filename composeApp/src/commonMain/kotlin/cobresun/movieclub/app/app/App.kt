package cobresun.movieclub.app.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.auth.presentation.AuthScreenRoot
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.club.presentation.ClubsScreenRoot
import cobresun.movieclub.app.core.domain.AsyncResult
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    return MaterialTheme(
        colorScheme = darkColorScheme(
            surface = Color(0xFF222831),
            onSurface = Color.White,
            primary = Color(0xFF2196F3),
            background = Color(0xFF222831),
            onBackground = Color.White
        ),
        content = content
    )
}

@Composable
fun App() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            // TODO: I don't like that this flashes the landing page first, this whole approach seems ugly?
            val authViewModel = koinViewModel<AuthViewModel>()
            val state by authViewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.user) {
                if (state.user is AsyncResult.Success) {
                    navController.navigate(Route.ClubGraph)
                }
            }

            NavHost(
                navController = navController,
                startDestination = Route.LandingPage,
            ) {
                composable<Route.LandingPage> {
                    LandingPageDestination(
                        onAuthClick = {
                            navController.navigate(Route.AuthGraph)
                        }
                    )
                }

                composable<Route.AuthGraph> {
                    AuthScreenRoot(
                        onAuthorized = {
                            navController.navigate(Route.ClubGraph)
                        }
                    )
                }

                composable<Route.ClubGraph> {
                    ClubsScreenRoot()
                }
            }
        }
    }
}

@Composable
fun LandingPageDestination(
    onAuthClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Landing Page",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onAuthClick
        ) {
            Text(text = "Auth")
        }
    }
}

@Preview
@Composable
fun LandingPageDestinationPreview() {
    AppTheme {
        Surface {
            LandingPageDestination(
                onAuthClick = {}
            )
        }
    }
}
