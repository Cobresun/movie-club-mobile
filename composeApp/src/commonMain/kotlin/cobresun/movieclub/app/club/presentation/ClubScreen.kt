package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
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
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.presentation.ReviewsScreen
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.presentation.WatchListScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubScreenRoot(
    viewModel: ClubViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ClubScreen(
        reviews = state.reviews,
        watchList = state.watchList,
        backlog = state.backlog,
        trendingMovies = state.trendingMovies,
        onAction = viewModel::onAction
    )
}

@Composable
fun ClubScreen(
    reviews: AsyncResult<List<Review>>,
    watchList: AsyncResult<List<WatchListItem>>,
    backlog: AsyncResult<List<WatchListItem>>,
    trendingMovies: AsyncResult<List<TmdbMovie>>,
    onAction: (ClubAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            selectedTab = pagerState.currentPage
        }
    }

    Column(modifier = modifier.statusBarsPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false // Must be turned off so allow the user to swipe to open ModalDrawer
        ) { index ->
            if (index == 0) {
                ReviewsScreen(reviews = reviews)
            } else {
                WatchListScreen(
                    watchList = watchList,
                    addMovieToWatchList = { movie -> onAction(ClubAction.OnAddMovieToWatchList(movie)) },
                    backlog = backlog,
                    addMovieToBacklog = { movie -> onAction(ClubAction.OnAddMovieToBacklog(movie)) },
                    onDeleteWatchListItem = { item -> onAction(ClubAction.OnDeleteWatchListItem(item)) },
                    onDeleteBacklogItem = { item -> onAction(ClubAction.OnDeleteBacklogItem(item)) },
                    onMoveToWatchList = { item -> onAction(ClubAction.OnMoveToWatchList(item)) },
                    trendingMovies = trendingMovies
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
                        onClick = { selectedTab = 0 },
                        text = { Text(text = "Reviews") }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(text = "Watch List") }
                    )
                }
            )
        }
    }
}
