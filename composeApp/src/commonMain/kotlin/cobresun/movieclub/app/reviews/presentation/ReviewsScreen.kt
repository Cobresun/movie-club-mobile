package cobresun.movieclub.app.reviews.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import cobresun.movieclub.app.reviews.presentation.components.ScoreChip
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewsScreenRoot(
    viewModel: ReviewsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReviewsScreen(
        reviews = state.reviews,
        modifier = Modifier.safeDrawingPadding(),
    )
}

@Composable
fun ReviewsScreen(
    reviews: List<Review>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(size = 192.dp),
        modifier = modifier
            .fillMaxSize(),
        horizontalArrangement = Arrangement.Center
    ) {
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

@Composable
fun ScoreGrid(scores: Map<User, Score>) {
    NonLazyGrid(
        columns = 2,
        itemCount = scores.size,
    ) {
        ScoreChip(
            user = scores.keys.elementAt(it),
            score = scores.values.elementAt(it).value,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
fun NonLazyGrid(
    columns: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    Column(modifier = modifier) {
        var rows = (itemCount / columns)
        if (itemCount.mod(columns) > 0) {
            rows += 1
        }

        for (rowId in 0 until rows) {
            val firstIndex = rowId * columns

            Row {
                for (columnId in 0 until columns) {
                    val index = firstIndex + columnId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (index < itemCount) {
                            content(index)
                        }
                    }
                }
            }
        }
    }
}
