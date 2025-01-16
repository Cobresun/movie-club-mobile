package cobresun.movieclub.app.watchlist.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.presentation.LIGHT_GRAY
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieClubModalBottomSheetLayout
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import kotlinx.coroutines.launch

@Composable
fun WatchListScreen(
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    MovieClubModalBottomSheetLayout(
        sheetContent = { AddMovieBottomSheetContent(trendingMovies = trendingMovies) },
        sheetState = sheetState,
    ) {
        Scaffold(
            modifier = modifier,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch { sheetState.show() }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "")
                }
            }
        ) { contentPadding ->
            ScreenContent(
                watchList = watchList,
                backlog = backlog,
                modifier = modifier.padding(contentPadding)
            )
        }
    }
}

@Composable
fun AddMovieBottomSheetContent(
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    modifier: Modifier = Modifier
) {
    AsyncResultHandler(
        asyncResult = trendingMovies,
        onSuccess = { movies ->
            LazyColumn(
                modifier = modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        text = "Trending",
                        fontWeight = FontWeight.Bold
                    )
                }

                items(items = movies, key = { it.id }) { movie ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .background(
                                color = Color(LIGHT_GRAY),
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(movie.title)
                                append(" ")
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append("(${movie.releaseYear})")
                                }
                            },
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ScreenContent(
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    modifier: Modifier = Modifier
) {
    var isShowingWatchList by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Header(
            isShowingWatchList = isShowingWatchList,
            searchQuery = searchQuery,
            toggleIsShowingWatchList = { isShowingWatchList = !isShowingWatchList },
            onSearchQueryChange = { searchQuery = it }
        )

        AnimatedMovieList(
            isShowingWatchList = isShowingWatchList,
            watchList = watchList,
            backlog = backlog,
            searchQuery = searchQuery
        )
    }
}

@Composable
private fun Header(
    isShowingWatchList: Boolean,
    searchQuery: String,
    toggleIsShowingWatchList: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isShowingWatchList) "Watch List" else "Backlog",
            modifier = Modifier.clickable { toggleIsShowingWatchList() },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        SearchBar(
            searchQuery = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnimatedMovieList(
    isShowingWatchList: Boolean,
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
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

@Composable
private fun WatchListGrid(
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
