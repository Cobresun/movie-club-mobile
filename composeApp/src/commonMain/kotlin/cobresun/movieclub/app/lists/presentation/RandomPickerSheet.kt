package cobresun.movieclub.app.lists.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import cobresun.movieclub.app.core.presentation.components.rememberAppSheetState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import cobresun.movieclub.app.lists.domain.RandomPick
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.reviews.presentation.EmphasizedEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Web `RandomPickerModal` as a bottom sheet: posters flick past, slowing
 * down, with a tick each, then land on [winner] with a wobble and a success
 * buzz. After a beat it offers "Make up next", "Never Mind" or "Move to…".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPickerSheet(
    candidates: List<Work>,
    winner: Work,
    noun: String,
    otherLists: List<WorkList>,
    hapticFeedback: HapticFeedback,
    onMakeUpNext: (Work) -> Unit,
    onMoveToList: (Work, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberAppSheetState()
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    var current by remember { mutableStateOf(candidates.randomOrNull() ?: winner) }
    var revealed by remember { mutableStateOf(false) }
    var showChoices by remember { mutableStateOf(false) }
    val nudge = remember { Animatable(0f) }
    val landed = remember { Animatable(0f) }

    LaunchedEffect(winner.id) {
        val pool = candidates.ifEmpty { listOf(winner) }
        for (cycle in 1..RandomPick.CYCLES) {
            current = pool.random()
            hapticFeedback.performTick()
            // Replay the nudge: the card drops in from slightly above (web `picker-tick`).
            if (!reducedMotion) launch {
                nudge.snapTo(1f)
                nudge.animateTo(0f, tween(120))
            }
            delay(RandomPick.intervalAfter(cycle))
        }
        current = winner
        revealed = true
        hapticFeedback.performSuccess()
        if (!reducedMotion) launch {
            landed.snapTo(0f)
            landed.animateTo(1f, tween(700, easing = EmphasizedEasing))
        }
        delay(RandomPick.REVEAL_PAUSE_MS)
        showChoices = true
    }

    fun closeThen(block: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            block()
        }
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedContent(
                targetState = revealed,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(200, easing = EmphasizedEasing), initialScale = 0.9f)) togetherWith
                        fadeOut(tween(150))
                },
                label = "picker-title",
            ) { landedTitle ->
                Text(
                    if (landedTitle) "Tonight's pick!" else "Picking a random $noun...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(170.dp).graphicsLayer {
                    translationY = -10.dp.toPx() * nudge.value
                    val (scale, rotation) = landedWobble(landed.value)
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                },
            ) {
                // The poster isn't re-keyed per tick, so it swaps in place instead of fading in each time.
                Poster(
                    current.imageUrl,
                    current.title,
                    Modifier.fillMaxWidth().then(
                        if (revealed) Modifier.border(3.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.small) else Modifier
                    ),
                )
                Text(
                    current.title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            AnimatedVisibility(
                visible = showChoices,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = EmphasizedEasing)) { it / 4 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { closeThen { onMakeUpNext(winner) } }) { Text("Make up next") }
                        OutlinedButton(onClick = { closeThen {} }) { Text("Never Mind") }
                    }
                    if (otherLists.isNotEmpty()) {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            FilledTonalButton(onClick = { menuOpen = true }) {
                                Text("Move to…")
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                otherLists.forEach { list ->
                                    DropdownMenuItem(
                                        text = { Text(list.title) },
                                        onClick = {
                                            menuOpen = false
                                            closeThen { onMoveToList(winner, list.id) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Web `picker-landed` keyframes as (scale, rotation°) at progress [t]:
 * 30% → 1.1 / -2°, 55% → 0.97 / 1°, 75% → 1.03 / 0°, then settle.
 */
private fun landedWobble(t: Float): Pair<Float, Float> {
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
    return when {
        t <= 0f || t >= 1f -> 1f to 0f
        t < 0.30f -> (t / 0.30f).let { lerp(1f, 1.1f, it) to lerp(0f, -2f, it) }
        t < 0.55f -> ((t - 0.30f) / 0.25f).let { lerp(1.1f, 0.97f, it) to lerp(-2f, 1f, it) }
        t < 0.75f -> ((t - 0.55f) / 0.20f).let { lerp(0.97f, 1.03f, it) to lerp(1f, 0f, it) }
        else -> ((t - 0.75f) / 0.25f).let { lerp(1.03f, 1f, it) to 0f }
    }
}
