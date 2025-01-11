package cobresun.movieclub.app.watchlist.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WatchListScreenRoot(
    viewModel: WatchListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    WatchListScreen(
        watchList = state.watchList
    )
}

@Composable
fun WatchListScreen(
    watchList: AsyncResult<List<WatchListItem>>,
    modifier: Modifier = Modifier,
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
            targetState = watchList,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { targetState ->
            AsyncResultHandler(
                asyncResult = targetState,
                onSuccess = {
                    WatchListGrid(
                        watchList = it,
                        modifier = modifier
                    )
                }
            )
        }
    }
}

@Composable
fun WatchListGrid(
    watchList: List<WatchListItem>,
    modifier: Modifier
) {
    MovieGrid(modifier = modifier) {
        items(items = watchList, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl
            ) {
                // TODO: Action buttons
            }
        }
    }
}
