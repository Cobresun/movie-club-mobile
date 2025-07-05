package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import cobresun.movieclub.app.app.AppTheme
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import cobresun.movieclub.app.reviews.presentation.components.AverageIconVector
import cobresun.movieclub.app.reviews.presentation.components.ScoreChip
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class ReviewScreenBottomSheetType {
    data object AddMovieSheet : ReviewScreenBottomSheetType()
    data class ReviewDetailsSheet(val review: Review) : ReviewScreenBottomSheetType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    reviews: AsyncResult<List<Review>>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    var openBottomSheet by remember { mutableStateOf<ReviewScreenBottomSheetType?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "Add Movie")
                    }
                }

                is ReviewScreenBottomSheetType.ReviewDetailsSheet -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = bottomSheetType.review.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    reviews: AsyncResult<List<Review>>,
    onSelectReviewItem: (Review) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        modifier = modifier,
                    )
                }
            )
        }
    }
}

@Composable
fun ReviewGrid(
    reviews: List<Review>,
    onSelectReviewItem: (Review) -> Unit,
    modifier: Modifier,
) {
    MovieGrid(modifier = modifier) {
        items(items = reviews, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl,
                modifier = Modifier.animateItem().clickable { onSelectReviewItem(it) }
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
        modifier = Modifier.fillMaxWidth(),
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
                        imageUrl = null
                    ) to Score(
                        id = "1",
                        value = 5.0,
                    ),
                    User(
                        id = "2",
                        name = "Brian",
                        imageUrl = null
                    ) to Score(
                        id = "2",
                        value = 5.0,
                    ),
                    User(
                        id = "3",
                        name = "Wesley",
                        imageUrl = null
                    ) to Score(
                        id = "3",
                        value = 5.0,
                    ),
                    User(
                        id = "4",
                        name = "Sunny",
                        imageUrl = null
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
                                imageUrl = null
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
                                imageUrl = null
                            ) to Score(
                                id = "1",
                                value = 5.0,
                            ),
                        )
                    )
                )
            ),
        )
    }
}
