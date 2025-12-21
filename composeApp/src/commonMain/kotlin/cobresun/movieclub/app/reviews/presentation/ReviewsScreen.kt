package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cobresun.movieclub.app.app.AppTheme
import cobresun.movieclub.app.core.data.dto.TmdbExternalDataDto
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.core.domain.extractYearFromDateString
import cobresun.movieclub.app.core.presentation.LIGHT_GRAY
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import cobresun.movieclub.app.reviews.presentation.components.AverageIconVector
import cobresun.movieclub.app.reviews.presentation.components.ReviewDetailsBottomSheetContent
import cobresun.movieclub.app.reviews.presentation.components.ScoreChip
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class ReviewScreenBottomSheetType {
    data object AddMovieSheet : ReviewScreenBottomSheetType()
    data class ReviewDetailsSheet(val review: Review) : ReviewScreenBottomSheetType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    reviews: AsyncResult<List<Review>>,
    watchList: AsyncResult<List<WatchListItem>>,
    currentUser: Member?,
    onDeleteReview: (String) -> Unit,
    onMoveToReview: (WatchListItem) -> Unit,
    onSubmitScore: (reviewWorkId: String, scoreId: String?, score: Double) -> Unit,
    isRefreshingReviews: Boolean = false,
    onRefreshReviews: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    var openBottomSheet by remember { mutableStateOf<ReviewScreenBottomSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openBottomSheet = ReviewScreenBottomSheetType.AddMovieSheet }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add movie to reviews"
                )
            }
        }
    ) { contentPadding ->
        ScreenContent(
            reviews = reviews,
            onSelectReviewItem = {
                openBottomSheet = ReviewScreenBottomSheetType.ReviewDetailsSheet(it)
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isRefreshingReviews = isRefreshingReviews,
            onRefreshReviews = onRefreshReviews,
            modifier = modifier
        )
    }

    openBottomSheet?.let { bottomSheetType ->
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = null },
            sheetState = sheetState
        ) {
            when (bottomSheetType) {
                is ReviewScreenBottomSheetType.AddMovieSheet -> {
                    AddMovieBottomSheetContent(
                        watchList = watchList,
                        onMoveToReview = { item ->
                            onMoveToReview(item)
                            openBottomSheet = null
                        }
                    )
                }

                is ReviewScreenBottomSheetType.ReviewDetailsSheet -> {
                    ReviewDetailsBottomSheetContent(
                        title = bottomSheetType.review.title,
                        createdDate = bottomSheetType.review.createdDate,
                        posterImageUrl = bottomSheetType.review.imageUrl,
                        scores = bottomSheetType.review.scores,
                        currentUser = currentUser,
                        onScoreSubmit = { scoreId, scoreValue ->
                            onSubmitScore(bottomSheetType.review.id, scoreId, scoreValue)
                            openBottomSheet = null
                        },
                        onDelete = {
                            onDeleteReview(bottomSheetType.review.id)
                            openBottomSheet = null
                        },
                        onShare = {
                            // TODO: Implement share functionality (copy URL to clipboard)
                        }
                    )
                }
            }
        }
    }
}

private fun extractReleaseYear(externalDataDto: TmdbExternalDataDto?): String? {
    return extractYearFromDateString(externalDataDto?.releaseDate)
}

@Composable
private fun AddMovieBottomSheetContent(
    watchList: AsyncResult<List<WatchListItem>>,
    onMoveToReview: (WatchListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncResultHandler(
        asyncResult = watchList,
        onSuccess = { movies ->
            LazyColumn(
                modifier = modifier.padding(16.dp)
            ) {
                item {
                    Text(
                        text = "From Watch List",
                        fontWeight = FontWeight.Bold
                    )
                }

                items(items = movies, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .background(
                                color = Color(LIGHT_GRAY),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onMoveToReview(item) }
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(item.title)
                                append(" ")
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    extractReleaseYear(item.externalDataDto)?.let {
                                        append("($it)")
                                    }
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
    reviews: AsyncResult<List<Review>>,
    onSelectReviewItem: (Review) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isRefreshingReviews: Boolean = false,
    onRefreshReviews: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var savedScrollPosition by remember { mutableStateOf<Int?>(null) }
    var previousSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        val isSearchStarting = previousSearchQuery.isEmpty() && searchQuery.isNotEmpty()
        val isSearchClearing = previousSearchQuery.isNotEmpty() && searchQuery.isEmpty()

        if (isSearchStarting) {
            savedScrollPosition = gridState.firstVisibleItemIndex
        } else if (isSearchClearing) {
            savedScrollPosition?.let { gridState.scrollToItem(it) }
        }

        previousSearchQuery = searchQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Reviews",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            SearchBar(
                searchQuery = searchQuery,
                onValueChange = { onSearchQueryChange(it) },
                modifier = Modifier.weight(1f)
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshingReviews,
            onRefresh = onRefreshReviews,
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = reviews,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { targetState ->
                AsyncResultHandler(
                    asyncResult = targetState,
                    onSuccess = { reviews ->
                        val filteredReviews = reviews.filter { review ->
                            review.title.contains(searchQuery.trim(), ignoreCase = true)
                        }

                        ReviewGrid(
                            reviews = filteredReviews,
                            onSelectReviewItem = onSelectReviewItem,
                            state = gridState,
                            modifier = modifier,
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ReviewGrid(
    reviews: List<Review>,
    onSelectReviewItem: (Review) -> Unit,
    state: LazyGridState,
    modifier: Modifier,
) {
    MovieGrid(
        modifier = modifier,
        state = state
    ) {
        items(items = reviews, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl,
                modifier = Modifier.animateItem(),
                onClick = { onSelectReviewItem(it) }
            ) {
                Text(
                    text = it.createdDate,
                )
                ScoreGrid(scores = it.scores)
            }
        }
    }
}

@Composable
fun ScoreGrid(scores: Map<User, Score>) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scores.forEach {
            val imageUrl = it.key.imageUrl

            if (imageUrl != null) {
                ScoreChip(
                    imageUrl = imageUrl,
                    contentDescription = it.key.name,
                    score = it.value.value,
                )
            } else {
                val initials = it.key.name.split(" ")

                if (initials.isEmpty()) return@forEach

                ScoreChip(
                    firstName = initials.first(),
                    lastName = if (initials.size > 1) initials.last() else null,
                    contentDescription = it.key.name,
                    score = it.value.value,
                )
            }
        }

        if (scores.isNotEmpty()) {
            ScoreChip(
                image = AverageIconVector,
                contentDescription = "Average",
                score = scores.values.map { it.value }.average(),
            )
        }
    }
}

@Preview
@Composable
fun ScoreGridPreview() {
    AppTheme {
        Surface {
            ScoreGrid(
                scores = mapOf(
                    User(
                        id = "1",
                        name = "Cole",
                        imageUrl = null,
                        email = "cole@example.com"
                    ) to Score(
                        id = "1",
                        value = 5.0,
                    ),
                    User(
                        id = "2",
                        name = "Brian",
                        imageUrl = null,
                        email = "brian@example.com"
                    ) to Score(
                        id = "2",
                        value = 5.0,
                    ),
                    User(
                        id = "3",
                        name = "Wesley",
                        imageUrl = null,
                        email = "wesley@example.com"
                    ) to Score(
                        id = "3",
                        value = 5.0,
                    ),
                    User(
                        id = "4",
                        name = "Sunny",
                        imageUrl = null,
                        email = "sunny@example.com"
                    ) to Score(
                        id = "4",
                        value = 5.0,
                    ),
                ),
            )
        }
    }
}

@Composable
@Preview
private fun ReviewsScreenPreview() {
    AppTheme {
        ReviewsScreen(
            reviews = AsyncResult.Success(emptyList()),
            watchList = AsyncResult.Success(emptyList()),
            currentUser = Member(
                id = "1",
                name = "John Doe",
                imageUrl = "https://image.tmdb.org/t/p/w500/q6y0oRbfh6IUxXp9C9AZ2UeFz5x.jpg",
                email = "john.mckinley@examplepetstore.com"
            ),
            onDeleteReview = {},
            onMoveToReview = {},
            onSubmitScore = { _, _, _ -> }
        )
    }
}

@Composable
@Preview
private fun ReviewsScreenWithDataPreview() {
    AppTheme {
        ReviewsScreen(
            reviews = AsyncResult.Success(
                listOf(
                    Review(
                        id = "1",
                        title = "The Lord of the Rings: The Fellowship of the Ring",
                        imageUrl = "https://www.themoviedb.org/t/p/w600_and_h900_bestv2/6oom5QYQ2yQzgp4bUS4eE2MvNte.jpg",
                        createdDate = "2021-01-01",
                        scores = mapOf(
                            User(
                                id = "1",
                                name = "Cole",
                                imageUrl = null,
                                email = "cole@example.com"
                            ) to Score(
                                id = "1",
                                value = 5.0,
                            ),
                        )
                    ),
                    Review(
                        id = "2",
                        title = "The Lord of the Rings: The Two Towers",
                        imageUrl = "https://image.tmdb.org/t/p/w154/null",
                        createdDate = "2021-01-01",
                        scores = mapOf(
                            User(
                                id = "1",
                                name = "Cole",
                                imageUrl = null,
                                email = "cole@example.com"
                            ) to Score(
                                id = "1",
                                value = 5.0,
                            ),
                        )
                    )
                )
            ),
            watchList = AsyncResult.Success(emptyList()),
            currentUser = Member(
                id = "1",
                name = "John Doe",
                imageUrl = "https://image.tmdb.org/t/p/w500/q6y0oRbfh6IUxXp9C9AZ2UeFz5x.jpg",
                email = "john.mckinley@examplepetstore.com"
            ),
            onDeleteReview = {},
            onMoveToReview = {},
            onSubmitScore = { _, _, _ -> }
        )
    }
}
