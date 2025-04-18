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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import kotlinx.coroutines.launch

sealed class WatchListBottomSheetType {
    data object AddMovie : WatchListBottomSheetType()
    data object WatchListItem : WatchListBottomSheetType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListScreen(
    watchList: AsyncResult<List<WatchListItem>>,
    addMovieToWatchList: (TmdbMovie) -> Unit,
    backlog: AsyncResult<List<WatchListItem>>,
    addMovieToBacklog: (TmdbMovie) -> Unit,
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    modifier: Modifier = Modifier,
) {
    var isShowingWatchList by remember { mutableStateOf(true) }
    var openBottomSheet by rememberSaveable { mutableStateOf<WatchListBottomSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { openBottomSheet = WatchListBottomSheetType.AddMovie }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "")
            }
        }
    ) { contentPadding ->
        ScreenContent(
            isShowingWatchList = isShowingWatchList,
            toggleIsShowingWatchList = { isShowingWatchList = !isShowingWatchList },
            watchList = watchList,
            backlog = backlog,
            modifier = modifier.padding(contentPadding)
        )
    }

    openBottomSheet?.let { bottomSheetType ->
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = null },
            sheetState = sheetState,
        ) {
            when (bottomSheetType) {
                WatchListBottomSheetType.AddMovie -> AddMovieBottomSheetContent(
                    trendingMovies = trendingMovies,
                    onSelectMovie = { movie ->
                        if (isShowingWatchList) {
                            addMovieToWatchList(movie)
                        } else {
                            addMovieToBacklog(movie)
                        }

                        openBottomSheet = null
                    }
                )
                WatchListBottomSheetType.WatchListItem -> TODO()
            }
        }
    }
}

@Composable
fun AddMovieBottomSheetContent(
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    onSelectMovie: (TmdbMovie) -> Unit = {},
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
                            .clickable { onSelectMovie(movie) }
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
    isShowingWatchList: Boolean,
    toggleIsShowingWatchList: () -> Unit,
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Header(
            isShowingWatchList = isShowingWatchList,
            searchQuery = searchQuery,
            toggleIsShowingWatchList = toggleIsShowingWatchList,
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
