package cobresun.movieclub.app.watchlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.MovieCard
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import kotlinx.coroutines.delay

/**
 * Full-screen overlay that displays shuffling movie posters.
 * Cycles through random movies with haptic feedback.
 *
 * @param watchList All available watch list items
 * @param isVisible Controls overlay visibility
 * @param selectedMovie Pre-selected movie for final animation frame
 * @param onComplete Callback when shuffle animation finishes
 * @param hapticFeedback Platform-specific haptic feedback implementation
 */
@Composable
fun ShuffleOverlay(
    watchList: List<WatchListItem>,
    isVisible: Boolean,
    selectedMovie: WatchListItem?,
    onComplete: () -> Unit,
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier
) {
    var currentDisplayIndex by remember { mutableIntStateOf(0) }

    // Animation constants
    val TOTAL_ITERATIONS = 6
    val ITERATION_DURATION_MS = 300L

    LaunchedEffect(isVisible) {
        if (isVisible && watchList.isNotEmpty()) {
            // Run shuffle iterations
            repeat(TOTAL_ITERATIONS) {
                // Get next random index, excluding current one to prevent consecutive duplicates
                val newIndex = watchList.indices
                    .filter { it != currentDisplayIndex }
                    .randomOrNull() ?: currentDisplayIndex

                currentDisplayIndex = newIndex
                hapticFeedback.performLightPulse()
                delay(ITERATION_DURATION_MS)
            }

            // Final iteration: land on selected movie
            selectedMovie?.let {
                val finalIndex = watchList.indexOfFirst { item -> item.id == it.id }
                if (finalIndex >= 0) {
                    currentDisplayIndex = finalIndex
                }
                hapticFeedback.performLightPulse()
                delay(ITERATION_DURATION_MS)
            }

            onComplete()
        }
    }

    if (isVisible && watchList.isNotEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .zIndex(1000f),
            contentAlignment = Alignment.Center
        ) {
            MovieCard(
                title = watchList[currentDisplayIndex].title,
                posterImageUrl = watchList[currentDisplayIndex].imageUrl,
                highlight = false,
                modifier = Modifier
                    .width(200.dp)
                    .wrapContentHeight()
            )
        }
    }
}
