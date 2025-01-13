package cobresun.movieclub.app.watchlist.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.watchlist.domain.WatchListItem

@Composable
fun WatchListScreen(
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    modifier: Modifier = Modifier,
) {
    var isShowingWatchList by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isShowingWatchList) "Watch List" else "Backlog",
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .clickable { isShowingWatchList = !isShowingWatchList },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            SearchBar(
                searchQuery = searchQuery,
                onValueChange = ({ searchQuery = it }),
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            )
        }

        AnimatedContent(
            targetState = if (isShowingWatchList) watchList else backlog,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { targetState ->
            AsyncResultHandler(
                asyncResult = targetState,
                onSuccess = { watchList ->
                    val filteredWatchList = watchList.filter { review ->
                        review.title.contains(searchQuery.trim(), ignoreCase = true)
                    }

                    WatchListGrid(
                        watchList = filteredWatchList,
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
                posterImageUrl = it.imageUrl,
                highlight = it.isNextMovie
            ) {
                // TODO: Action buttons
            }
        }
    }
}
