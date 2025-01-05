package cobresun.movieclub.app.reviews.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.reviews.domain.Review
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewsScreenRoot(
    viewModel: ReviewsScreenViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReviewsScreen(
        reviews = state
    )
}

@Composable
fun ReviewsScreen(
    reviews: List<Review>
) {
    LazyVerticalGrid(
        columns = GridCells.FixedSize(size = 192.dp),
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(items = reviews, key = { it.id }) {
            MovieCard(
                title = it.title,
                posterImageUrl = it.imageUrl,
            ) {
                Text(text = it.createdDate)
            }
        }
    }
}
