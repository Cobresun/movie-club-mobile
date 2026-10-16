package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Poster-grid loading placeholder that matches the real grid's rhythm. */
@Composable
fun GridSkeleton(modifier: Modifier = Modifier, minCellWidth: Dp = 104.dp) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellWidth),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        items(12) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT))
                ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp))
            }
        }
    }
}
