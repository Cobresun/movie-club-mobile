package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.app.AppTheme
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.presentation.ReviewsScreen
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.presentation.WatchListScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubScreenRoot(
    viewModel: ClubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                ClubNavigationEvent.NavigateToReviewsTab -> {
                    selectedTab = 0
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ClubAction.OnClearError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ClubScreen(
            reviews = state.reviews,
            watchList = state.watchList,
            backlog = state.backlog,
            trendingMovies = state.trendingMovies,
            currentUser = state.currentUser,
            state = state,
            onAction = viewModel::onAction,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun ClubScreen(
    reviews: AsyncResult<List<Review>>,
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    currentUser: Member?,
    state: ClubState,
    onAction: (ClubAction) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = selectedTab) { 2 }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            onTabSelected(pagerState.currentPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false // Must be turned off so allow the user to swipe to open ModalDrawer
        ) { index ->
            if (index == 0) {
                ReviewsScreen(
                    reviews = reviews,
                    watchList = watchList,
                    currentUser = currentUser,
                    onDeleteReview = { reviewId -> onAction(ClubAction.OnDeleteReview(reviewId)) },
                    onMoveToReview = { item -> onAction(ClubAction.OnMoveToReview(item)) },
                    onSubmitScore = { reviewWorkId, scoreId, scoreValue ->
                        onAction(ClubAction.OnSubmitScore(reviewWorkId, scoreId, scoreValue))
                    },
                    onShareReview = { reviewId -> onAction(ClubAction.OnShareReview(reviewId)) },
                    isRefreshingReviews = state.isRefreshingReviews,
                    onRefreshReviews = { onAction(ClubAction.OnRefreshReviews) }
                )
            } else {
                WatchListScreen(
                    watchList = watchList,
                    backlog = backlog,
                    addMovieToWatchList = { movie -> onAction(ClubAction.OnAddMovieToWatchList(movie)) },
                    addMovieToBacklog = { movie -> onAction(ClubAction.OnAddMovieToBacklog(movie)) },
                    onDeleteWatchListItem = { item -> onAction(ClubAction.OnDeleteWatchListItem(item)) },
                    onDeleteBacklogItem = { item -> onAction(ClubAction.OnDeleteBacklogItem(item)) },
                    onMoveToWatchList = { item -> onAction(ClubAction.OnMoveToWatchList(item)) },
                    onMoveToReview = { item -> onAction(ClubAction.OnMoveToReview(item)) },
                    onSetNextWatch = { item -> onAction(ClubAction.OnSetNextWatch(item)) },
                    trendingMovies = trendingMovies,
                    isRefreshingWatchList = state.isRefreshingWatchList,
                    isRefreshingBacklog = state.isRefreshingBacklog,
                    onRefreshWatchList = { onAction(ClubAction.OnRefreshWatchList) },
                    onRefreshBacklog = { onAction(ClubAction.OnRefreshBacklog) }
                )
            }
        }

        Box(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.navigationBarsPadding(),
                indicator = {},
                divider = {},
                tabs = {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text(text = "Reviews") }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text(text = "Watch List") }
                    )
                }
            )
        }
    }
}

@Composable
@Preview
private fun ClubScreenPreview() {
    AppTheme {
        ClubScreen(
            reviews = AsyncResult.Success(
                listOf(
                    Review(
                        id = "1",
                        title = "The Shawshank Redemption",
                        imageUrl = "https://image.tmdb.org/t/p/w500/q6y0oRbfh6IUxXp9C9AZ2UeFz5x.jpg",
                        createdDate = "2023-01-15",
                        scores = emptyMap()
                    ),
                    Review(
                        id = "2",
                        title = "The Godfather",
                        imageUrl = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                        createdDate = "2023-02-20",
                        scores = emptyMap()
                    )
                )
            ),
            watchList = AsyncResult.Success(
                listOf(
                    WatchListItem(
                        id = "1",
                        type = WorkType.MOVIE,
                        title = "Pulp Fiction",
                        createdDate = "2023-03-10",
                        externalId = "680",
                        imageUrl = "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszBLlWP9PHTdsgJ.jpg",
                        externalDataDto = null,
                        isNextMovie = false
                    ),
                    WatchListItem(
                        id = "2",
                        type = WorkType.MOVIE,
                        title = "The Dark Knight",
                        createdDate = "2023-04-05",
                        externalId = "155",
                        imageUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUD9CkiRZI9A6UzmiQNx.jpg",
                        externalDataDto = null,
                        isNextMovie = false
                    )
                )
            ),
            backlog = AsyncResult.Success(
                listOf(
                    WatchListItem(
                        id = "3",
                        type = WorkType.MOVIE,
                        title = "Forrest Gump",
                        createdDate = "2023-05-01",
                        externalId = "13",
                        imageUrl = "https://image.tmdb.org/t/p/w500/arw2FuxgC2LzJgR2g222z222z222z222.jpg",
                        externalDataDto = null,
                        isNextMovie = false
                    ),
                    WatchListItem(
                        id = "4",
                        type = WorkType.MOVIE,
                        title = "Inception",
                        createdDate = "2023-06-12",
                        externalId = "27205",
                        imageUrl = "https://image.tmdb.org/t/p/w500/s3TBrRGB1EO7Wk0i222z222z222z222.jpg",
                        externalDataDto = null,
                        isNextMovie = false
                    )
                )
            ),
            trendingMovies = AsyncResult.Success(
                listOf(
                    TmdbMovie(
                        id = 1,
                        title = "Dune: Part Two",
                        releaseYear = "2024",
                        imageUrl = "/czQnC1tXz222z222z222z222z222z222.jpg",
                        popularity = 123.45
                    ),
                    TmdbMovie(
                        id = 2,
                        title = "Godzilla x Kong: The New Empire",
                        releaseYear = "2024",
                        imageUrl = "/gKkl3z222z222z222z222z222z222z222.jpg",
                        popularity = 98.76
                    )
                )
            ),
            currentUser = Member(
                id = "1",
                name = "John Doe",
                imageUrl = "https://image.tmdb.org/t/p/w500/q6y0oR",
                email = "john.mclean@examplepetstore.com"
            ),
            state = ClubState(
                reviews = AsyncResult.Success(emptyList()),
                watchList = AsyncResult.Success(emptyList()),
                backlog = AsyncResult.Success(emptyList()),
                trendingMovies = AsyncResult.Success(emptyList()),
                currentUser = null,
                isRefreshingReviews = false,
                isRefreshingWatchList = false,
                isRefreshingBacklog = false
            ),
            onAction = {},
            selectedTab = 0,
            onTabSelected = {}
        )
    }
}
