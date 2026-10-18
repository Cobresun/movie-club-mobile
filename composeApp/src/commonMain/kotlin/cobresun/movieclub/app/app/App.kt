package cobresun.movieclub.app.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import cobresun.movieclub.app.auth.presentation.AuthAction
import cobresun.movieclub.app.auth.presentation.AuthScreenRoot
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.club.presentation.ClubsScreenRoot
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.presentation.AppearanceTheme
import cobresun.movieclub.app.core.presentation.AppearanceViewModel
import cobresun.movieclub.app.core.presentation.ConfigureImageLoader
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    ConfigureImageLoader()
    AppearanceTheme(koinViewModel<AppearanceViewModel>()) {
        Scaffold(
            contentWindowInsets = WindowInsets()
        ) { paddingValues ->
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
                        if (authState.serverUnavailable) {
                            ServiceUnavailableScreen(
                                onRetry = { authViewModel.onAction(AuthAction.RetryStartup) }
                            )
                        } else {
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
}

@Composable
private fun AppNavigation(
    isAuthenticated: Boolean,
    authViewModel: AuthViewModel
) {
    val backStack = rememberRouteBackStack(if (isAuthenticated) Route.ClubGraph else Route.LandingPage)

    // Handle logout - back to the landing page, with nothing to go back to
    LaunchedEffect(Unit) {
        authViewModel.navigationEvents.collect { event ->
            when (event) {
                is AuthViewModel.AuthNavigationEvent.NavigateToLandingPage -> backStack.resetTo(Route.LandingPage)
            }
        }
    }

    RouteDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Route.LandingPage> {
                LandingPageScreen(onAuthClick = { backStack.push(Route.AuthGraph) })
            }

            entry<Route.AuthGraph> {
                AuthScreenRoot(
                    // Drops LandingPage and AuthGraph: back leaves the app
                    onAuthorized = { backStack.resetTo(Route.ClubGraph) }
                )
            }

            entry<Route.ClubGraph> {
                ClubsScreenRoot(authViewModel = authViewModel)
            }
        },
    )
}
