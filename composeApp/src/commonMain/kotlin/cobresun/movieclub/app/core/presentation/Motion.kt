package cobresun.movieclub.app.core.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Web motion tokens (`--ease-emphasized`, `--duration-*`). */
object AppMotion {
    val Emphasized = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val FAST = 150
    const val BASE = 200
    const val SLOW = 300
}

/**
 * Fades in while rising 16dp the first time it's composed (web `animate-fade-up`).
 * Shows instantly when the system asks for reduced motion.
 */
fun Modifier.fadeUpOnAppear(durationMillis: Int = AppMotion.SLOW, delayMillis: Int = 0): Modifier = composed {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis, delayMillis, easing = AppMotion.Standard))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }
}

/**
 * Screen-to-screen transitions for NavHosts. navigation-compose defaults to a
 * 700ms crossfade, which keeps two full screens composed and drawing long
 * enough to read as lag.
 */
object NavMotion {
    val enter: androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
    val exit: androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))
}
