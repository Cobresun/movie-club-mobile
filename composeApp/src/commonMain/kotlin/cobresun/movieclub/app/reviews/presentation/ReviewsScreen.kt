package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
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
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.core.presentation.components.SearchBar
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import cobresun.movieclub.app.reviews.presentation.components.AverageIconVector
import cobresun.movieclub.app.reviews.presentation.components.ScoreChip

@Composable
fun ReviewsScreen(
    reviews: AsyncResult<List<Review>>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

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
                onValueChange = ({ searchQuery = it }),
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
    modifier: Modifier,
) {
    MovieGrid(modifier = modifier) {
        items(items = reviews, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl,
                modifier = Modifier.animateItem()
            ) {
                Text(
                    text = it.createdDate,
                )
                ScoreGrid(scores = it.scores)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
