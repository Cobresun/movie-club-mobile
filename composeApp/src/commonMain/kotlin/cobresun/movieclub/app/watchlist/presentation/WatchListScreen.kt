package cobresun.movieclub.app.watchlist.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WatchListScreenRoot(
    viewModel: WatchListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    WatchListScreen(
        state = state,
        modifier = Modifier.safeDrawingPadding(),
    )
}

@Composable
fun WatchListScreen(
    state: WatchListState,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Watch List",
            modifier = Modifier.padding(vertical = 16.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        AnimatedContent(
            targetState = state,
            modifier = Modifier.fillMaxSize()
        ) { targetState ->
            when (targetState) {
                is WatchListState.Error -> {
                    Text(
                        text = targetState.errorMessage,
                        color = Color.Red
                    )
                }

                is WatchListState.Loaded -> {} // TODO

                is WatchListState.Loading -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
