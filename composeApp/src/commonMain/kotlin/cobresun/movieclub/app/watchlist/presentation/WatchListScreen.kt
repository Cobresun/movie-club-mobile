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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cobresun.movieclub.app.theme.AppTheme
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.platform.createNoOpHapticFeedback

import cobresun.movieclub.app.core.presentation.components.MovieActionBottomSheetContent
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class WatchListBottomSheetType {
    data object AddMovieSheet : WatchListBottomSheetType()
    data class WatchListItemSheet(val watchListItem: WatchListItem) : WatchListBottomSheetType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchListScreen(
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    addMovieToWatchList: (TmdbMovie) -> Unit,
    addMovieToBacklog: (TmdbMovie) -> Unit,
    onDeleteWatchListItem: (WatchListItem) -> Unit,
    onDeleteBacklogItem: (WatchListItem) -> Unit,
    onMoveToWatchList: (WatchListItem) -> Unit,
    onMoveToReview: (WatchListItem) -> Unit,
    onSetNextWatch: (WatchListItem) -> Unit,
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    isRefreshingWatchList: Boolean = false,
    isRefreshingBacklog: Boolean = false,
    isAddingToBacklog: Boolean = false,
    onRefreshWatchList: () -> Unit = {},
    onRefreshBacklog: () -> Unit = {},
    isShowingWatchList: Boolean = true,
    onToggleWatchListView: () -> Unit = {},
    isShufflingWatchList: Boolean = false,
    shuffleSelectedMovie: WatchListItem? = null,
    onRandomizeWatchList: () -> Unit = {},
    onShuffleComplete: () -> Unit = {},
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    var openBottomSheet by rememberSaveable { mutableStateOf<WatchListBottomSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isAddingToBacklog) {
                        openBottomSheet = WatchListBottomSheetType.AddMovieSheet
                    }
                }
            ) {
                if (isAddingToBacklog) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add movie to ${if (isShowingWatchList) "watch list" else "backlog"}"
                    )
                }
            }
        }
    ) { contentPadding ->
        Box {
            ScreenContent(
                isShowingWatchList = isShowingWatchList,
                toggleIsShowingWatchList = onToggleWatchListView,
                watchList = watchList,
                backlog = backlog,
                onSelectWatchListItem = {
                    openBottomSheet = WatchListBottomSheetType.WatchListItemSheet(it)
                },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                isRefreshingWatchList = isRefreshingWatchList,
                isRefreshingBacklog = isRefreshingBacklog,
                onRefreshWatchList = onRefreshWatchList,
                onRefreshBacklog = onRefreshBacklog,
                isShufflingWatchList = isShufflingWatchList,
                onRandomizeWatchList = onRandomizeWatchList
            )

            // Shuffle overlay
            ShuffleOverlay(
                watchList = (watchList as? AsyncResult.Success)?.data ?: emptyList(),
                isVisible = isShufflingWatchList,
                selectedMovie = shuffleSelectedMovie,
                onComplete = onShuffleComplete,
                hapticFeedback = hapticFeedback
            )
        }
    }

    openBottomSheet?.let { bottomSheetType ->
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = null },
            sheetState = sheetState,
        ) {
            when (bottomSheetType) {
                is WatchListBottomSheetType.AddMovieSheet -> {
                    AddMovieBottomSheetContent(
                        trendingMovies = trendingMovies,
                        onSelectMovie = { movie ->
                            if (isShowingWatchList) {
                                // For now this just adds to the backlog
                                addMovieToWatchList(movie)
                                onToggleWatchListView()
                            } else {
                                addMovieToBacklog(movie)
                            }

                            openBottomSheet = null
                        }
                    )
                }

                is WatchListBottomSheetType.WatchListItemSheet -> {
                    MovieActionBottomSheetContent(
                        title = bottomSheetType.watchListItem.title,
                        onDelete = {
                            if (isShowingWatchList) {
                                onDeleteWatchListItem(bottomSheetType.watchListItem)
                            } else {
                                onDeleteBacklogItem(bottomSheetType.watchListItem)
                            }
                            openBottomSheet = null
                        },
                        primaryButtonText = if (isShowingWatchList) "Review" else "Move to watch list",
                        onPrimaryButtonClick = {
                            if (isShowingWatchList) {
                                onMoveToReview(bottomSheetType.watchListItem)
                            } else {
                                onMoveToWatchList(bottomSheetType.watchListItem)
                            }
                            openBottomSheet = null
                        },
                        secondaryButtonText = if (isShowingWatchList && !bottomSheetType.watchListItem.isNextMovie) {
                            "Next watch"
                        } else {
                            null
                        },
                        onSecondaryButtonClick = {
                            onSetNextWatch(bottomSheetType.watchListItem)
                            openBottomSheet = null
                        }
                    )
                }
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
                                color = MaterialTheme.colorScheme.surfaceVariant,
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
    onSelectWatchListItem: (WatchListItem) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isRefreshingWatchList: Boolean = false,
    isRefreshingBacklog: Boolean = false,
    onRefreshWatchList: () -> Unit = {},
    onRefreshBacklog: () -> Unit = {},
    isShufflingWatchList: Boolean = false,
    onRandomizeWatchList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Header(
            isShowingWatchList = isShowingWatchList,
            searchQuery = searchQuery,
            toggleIsShowingWatchList = toggleIsShowingWatchList,
            onSearchQueryChange = { onSearchQueryChange(it) },
            isShufflingWatchList = isShufflingWatchList,
            onRandomizeWatchList = onRandomizeWatchList
        )

        AnimatedMovieList(
            isShowingWatchList = isShowingWatchList,
            watchList = watchList,
            backlog = backlog,
            onSelectWatchListItem = onSelectWatchListItem,
            searchQuery = searchQuery,
            isRefreshingWatchList = isRefreshingWatchList,
            isRefreshingBacklog = isRefreshingBacklog,
            onRefreshWatchList = onRefreshWatchList,
            onRefreshBacklog = onRefreshBacklog
        )
    }
}

@Composable
private fun Header(
    isShowingWatchList: Boolean,
    searchQuery: String,
    toggleIsShowingWatchList: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    isShufflingWatchList: Boolean = false,
    onRandomizeWatchList: () -> Unit = {}
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

        // Dice button - only show on watch list tab
        if (isShowingWatchList) {
            IconButton(
                onClick = {
                    if (!isShufflingWatchList) {
                        onRandomizeWatchList()
                    }
                },
                enabled = !isShufflingWatchList
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = "Randomize watch list",
                    tint = if (isShufflingWatchList)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnimatedMovieList(
    isShowingWatchList: Boolean,
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    onSelectWatchListItem: (WatchListItem) -> Unit,
    searchQuery: String,
    isRefreshingWatchList: Boolean = false,
    isRefreshingBacklog: Boolean = false,
    onRefreshWatchList: () -> Unit = {},
    onRefreshBacklog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val watchListGridState = rememberLazyGridState()
    val backlogGridState = rememberLazyGridState()

    var watchListSavedScrollPosition by remember { mutableStateOf<Int?>(null) }
    var backlogSavedScrollPosition by remember { mutableStateOf<Int?>(null) }
    var previousSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        val isSearchStarting = previousSearchQuery.isEmpty() && searchQuery.isNotEmpty()
        val isSearchClearing = previousSearchQuery.isNotEmpty() && searchQuery.isEmpty()

        if (isSearchStarting) {
            // Save current position for both lists
            watchListSavedScrollPosition = watchListGridState.firstVisibleItemIndex
            backlogSavedScrollPosition = backlogGridState.firstVisibleItemIndex
        } else if (isSearchClearing) {
            // Restore position for the currently visible list
            if (isShowingWatchList) {
                watchListSavedScrollPosition?.let { watchListGridState.scrollToItem(it) }
            } else {
                backlogSavedScrollPosition?.let { backlogGridState.scrollToItem(it) }
            }
        }

        previousSearchQuery = searchQuery
    }

    val isRefreshing = if (isShowingWatchList) isRefreshingWatchList else isRefreshingBacklog
    val onRefresh = if (isShowingWatchList) onRefreshWatchList else onRefreshBacklog

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = isShowingWatchList,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { showingWatchList ->
            val currentList = if (showingWatchList) watchList else backlog
            val currentGridState = if (showingWatchList) watchListGridState else backlogGridState

            AsyncResultHandler(
                asyncResult = currentList,
                onSuccess = { items ->
                    val filteredItems = items.filter { item ->
                        item.title.contains(searchQuery.trim(), ignoreCase = true)
                    }

                    WatchListGrid(
                        watchList = filteredItems,
                        onSelectWatchListItem = onSelectWatchListItem,
                        state = currentGridState,
                        modifier = modifier
                    )
                }
            )
        }
    }
}

@Composable
private fun WatchListGrid(
    watchList: List<WatchListItem>,
    onSelectWatchListItem: (WatchListItem) -> Unit,
    state: LazyGridState,
    modifier: Modifier
) {
    MovieGrid(
        modifier = modifier,
        state = state
    ) {
        items(items = watchList, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl,
                highlight = it.isNextMovie,
                modifier = Modifier.animateItem(),
                onClick = { onSelectWatchListItem(it) }
            )
        }
    }
}

@Composable
@Preview
private fun WatchListScreenPreview() {
    AppTheme {
        WatchListScreen(
            watchList = AsyncResult.Success(emptyList()),
            addMovieToWatchList = {},
            backlog = AsyncResult.Success(emptyList()),
            addMovieToBacklog = {},
            onDeleteWatchListItem = {},
            onDeleteBacklogItem = {},
            onMoveToWatchList = {},
            onMoveToReview = {},
            onSetNextWatch = {},
            trendingMovies = AsyncResult.Success(emptyList()),
            hapticFeedback = createNoOpHapticFeedback()
        )
    }
}

@Composable
@Preview
private fun WatchListScreenWithDataPreview() {
    AppTheme {
        WatchListScreen(
            watchList = AsyncResult.Success(
                listOf(
                    WatchListItem(
                        id = "1",
                        type = WorkType.MOVIE,
                        title = "The Lord of the Rings: The Fellowship of the Ring",
                        createdDate = "2021-01-01",
                        externalId = "1",
                        imageUrl = "https://www.themoviedb.org/t/p/w600_and_h900_bestv2/6oom5QYQ2yQzgp4bUS4eE2MvNte.jpg",
                        externalDataDto = null,
                        isNextMovie = false,
                    )
                )
            ),
            addMovieToWatchList = {},
            backlog = AsyncResult.Success(emptyList()),
            addMovieToBacklog = {},
            onDeleteWatchListItem = {},
            onDeleteBacklogItem = {},
            onMoveToWatchList = {},
            onMoveToReview = {},
            onSetNextWatch = {},
            trendingMovies = AsyncResult.Success(emptyList()),
            hapticFeedback = createNoOpHapticFeedback()
        )
    }
}

@Composable
@Preview
private fun AddMovieBottomSheetContentPreview() {
    AppTheme {
        AddMovieBottomSheetContent(
            trendingMovies = AsyncResult.Success(emptyList()),
        )
    }
}

@Composable
@Preview
private fun AddMovieBottomSheetContentWithDataPreview() {
    AppTheme {
        AddMovieBottomSheetContent(
            trendingMovies = AsyncResult.Success(
                listOf(
                    TmdbMovie(
                        id = 1,
                        title = "The Lord of the Rings: The Fellowship of the Ring",
                        releaseYear = "2001",
                        imageUrl = "/6oom5QYQ2yQzgp4bUS4eE2MvNte.jpg",
                        popularity = 0.0,
                    )
                )
            ),
        )
    }
}

@Composable
@Preview
private fun MovieActionBottomSheetContentPreview() {
    AppTheme {
        MovieActionBottomSheetContent(
            title = "The Lord of the Rings: The Fellowship of the Ring",
            onDelete = {},
            primaryButtonText = "Review",
            onPrimaryButtonClick = {},
        )
    }
}
