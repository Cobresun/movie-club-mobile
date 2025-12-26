package cobresun.movieclub.app.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cobresun.movieclub.app.auth.presentation.AuthScreenRoot
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.club.presentation.ClubsScreenRoot
import cobresun.movieclub.app.core.domain.AsyncResult
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
        Scaffold { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                val authViewModel = koinViewModel<AuthViewModel>()
                val authState by authViewModel.state.collectAsStateWithLifecycle()

                when (authState.user) {
                    is AsyncResult.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AsyncResult.Success, is AsyncResult.Error -> {
                        val isAuthenticated = authState.user is AsyncResult.Success
                        AppNavigation(
                            isAuthenticated = isAuthenticated,
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(
    isAuthenticated: Boolean,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()

    // Handle logout - navigate to landing page when navigation event received
    LaunchedEffect(Unit) {
        authViewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthViewModel.AuthNavigationEvent.NavigateToLandingPage -> {
                    navController.navigate(Route.LandingPage) {
                        // Clear entire backstack when logging out
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Route.ClubGraph else Route.LandingPage,
    ) {
        composable<Route.LandingPage> {
            LandingPageScreen(
                onAuthClick = {
                    navController.navigate(Route.AuthGraph)
                }
            )
        }

        composable<Route.AuthGraph> {
            AuthScreenRoot(
                onAuthorized = {
                    navController.navigate(Route.ClubGraph) {
                        // Clear entire backstack including LandingPage and AuthGraph
                        popUpTo(Route.LandingPage) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<Route.ClubGraph> {
            ClubsScreenRoot(authViewModel = authViewModel)
        }
    }
}
