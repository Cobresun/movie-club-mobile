package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.AsyncResultHandler
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.core.presentation.components.MovieGrid
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import cobresun.movieclub.app.reviews.presentation.components.AverageIconVector
import cobresun.movieclub.app.reviews.presentation.components.ScoreChip
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewsScreenRoot(
    viewModel: ReviewsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReviewsScreen(reviews = state.reviews)
}

@Composable
fun ReviewsScreen(
    reviews: AsyncResult<List<Review>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Reviews",
            modifier = Modifier.padding(vertical = 16.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        AnimatedContent(
            targetState = reviews,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { targetState ->
            AsyncResultHandler(
                asyncResult = targetState,
                onSuccess = {
                    ReviewGrid(
                        reviews = it,
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
            ) {
                Text(
                    text = it.createdDate,
                    color = Color.White,
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

        ScoreChip(
            image = AverageIconVector,
            contentDescription = "Average",
            score = scores.values.map { it.value }.average(),
        )
    }
}
