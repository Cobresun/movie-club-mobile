package cobresun.movieclub.app.lists.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.Poster
import kotlinx.coroutines.delay

/**
 * Slot-machine style random pick: posters flick past, slowing down, before
 * landing on [selected]. Calls [onFinished] after a short reveal.
 */
@Composable
fun ShuffleOverlay(
    candidates: List<Work>,
    selected: Work,
    onFinished: () -> Unit,
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf(candidates.random()) }
    var landed by remember { mutableStateOf(false) }

    LaunchedEffect(selected.id) {
        val pool = candidates.ifEmpty { listOf(selected) }
        val steps = 10
        repeat(steps) { i ->
            current = pool.filter { it.id != current.id }.randomOrNull() ?: current
            hapticFeedback.performLightPulse()
            delay(90L + i * i * 6L) // ease out
        }
        current = selected
        landed = true
        hapticFeedback.performLightPulse()
        delay(1400)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.88f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (landed) onFinished()
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                if (landed) "Tonight's pick" else "Shuffling…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    if (landed) {
                        scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 0.85f) + fadeIn() togetherWith fadeOut(tween(80))
                    } else {
                        fadeIn(tween(60)) togetherWith fadeOut(tween(60))
                    }
                },
                contentKey = { it.id },
            ) { work ->
                Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 24.dp) {
                    Poster(work.imageUrl, work.title, Modifier.width(210.dp), MaterialTheme.shapes.medium, highlighted = landed)
                }
            }
            Text(
                current.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}
