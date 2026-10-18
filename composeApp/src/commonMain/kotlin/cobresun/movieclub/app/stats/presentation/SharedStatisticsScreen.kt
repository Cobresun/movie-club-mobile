package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.toUserMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SharedStatisticsScreenRoot(onBack: () -> Unit, viewModel: StatsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onAction(StatsAction.Shown) }
    SharedStatisticsScreen(state, onBack = onBack, onRetry = { viewModel.onAction(StatsAction.Refresh) })
}

/**
 * Web `SharedStatisticsView`: a shared statistics link for a club the user isn't
 * in, showing the same widgets read-only under the club's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedStatisticsScreen(state: StatsState, onBack: () -> Unit, onRetry: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "STATISTICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            state.clubName,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when (val data = state.data) {
                is AsyncResult.Loading -> StatsSkeleton()
                is AsyncResult.Error -> ErrorState(
                    message = (data.dataError as? DataError.Remote)?.toUserMessage("load statistics") ?: "Couldn't load statistics.",
                    onRetry = onRetry,
                )
                is AsyncResult.Success -> StatsContent(
                    state = state,
                    data = data.data,
                    onGoToReviews = null,
                    onOpenWrapped = null,
                    bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                )
            }
        }
    }
}
