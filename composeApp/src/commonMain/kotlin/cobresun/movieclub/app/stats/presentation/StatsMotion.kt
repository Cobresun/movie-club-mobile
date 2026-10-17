package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * The statistics page's motion, after the web's `v-reveal`, `animate-bar-grow`
 * and `useCountUp`: widgets fade up the first time they scroll into view, their
 * bars grow in row by row, and headline numbers count up. A widget that has
 * already been seen shows its final state when it scrolls back.
 */

private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val Emphasized = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val EaseOutCubic = Easing { t -> 1f - (1f - t).pow(3) }

/** Whether content inside the current widget should animate in (false once it has been seen). */
internal val LocalAnimateIn = compositionLocalOf { false }

/**
 * Fades and lifts a widget into place the first time it is composed (a lazy
 * list composes it as it scrolls into view). Returns whether its content
 * should play its own entrance, for [LocalAnimateIn].
 */
@Composable
internal fun rememberReveal(): Pair<Modifier, Boolean> {
    val reduced = rememberReducedMotion()
    // Saved with the lazy item, so a widget scrolled away and back stays revealed.
    var seen by rememberSaveable { mutableStateOf(false) }
    val animateIn = remember { !seen && !reduced }
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        seen = true
        if (progress.value < 1f) progress.animateTo(1f, tween(300, easing = StandardEasing))
    }
    val modifier = Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }
    return modifier to animateIn
}

/** A bar's growth from 0 to 1, staggered 60ms per [index] (web `animate-bar-grow`). */
@Composable
internal fun rememberBarGrowth(index: Int): Float {
    val animateIn = LocalAnimateIn.current
    val growth = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (growth.value < 1f) {
            delay(index * 60L)
            growth.animateTo(1f, tween(700, easing = Emphasized))
        }
    }
    return growth.value
}

/**
 * Counts up from zero to [target] over 900ms the first time a widget is shown,
 * then follows [target] directly (web `useCountUp`).
 */
@Composable
internal fun rememberCountUp(target: Int): Int {
    val animateIn = LocalAnimateIn.current
    val latest by rememberUpdatedState(target)
    var shown by remember { mutableStateOf(if (animateIn) 0 else target) }
    var counting by remember { mutableStateOf(animateIn) }
    LaunchedEffect(Unit) {
        if (!counting) return@LaunchedEffect
        val progress = Animatable(0f)
        // Read the target every frame so data that lands mid-count is where it ends.
        progress.animateTo(1f, tween(900, easing = EaseOutCubic)) { shown = (latest * value).roundToInt() }
        counting = false
    }
    return if (counting) shown else target
}
