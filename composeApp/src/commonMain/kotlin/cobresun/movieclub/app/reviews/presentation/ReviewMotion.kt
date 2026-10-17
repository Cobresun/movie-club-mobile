package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Web `--ease-emphasized`: quick start, long gentle settle. */
internal val EmphasizedEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** Fades in while rising a few dp the first time it's composed (empty states). */
internal fun Modifier.fadeUpOnEnter(durationMillis: Int = 300, delayMillis: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis, delayMillis, easing = EmphasizedEasing)) }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12.dp.toPx()
    }
}

/**
 * Fades in from a slight shrink when [entering] (web gallery `enter-from`:
 * opacity 0, scale .95). Off for items that were already on screen, so cards
 * scrolled into view don't animate.
 */
internal fun Modifier.scaleInOnEnter(entering: Boolean, from: Float = 0.95f, durationMillis: Int = 200): Modifier =
    if (!entering) this else composed {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis, easing = EmphasizedEasing)) }
        graphicsLayer {
            alpha = progress.value
            val scale = from + (1f - from) * progress.value
            scaleX = scale
            scaleY = scale
        }
    }
