package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.stats.domain.WrappedCard
import cobresun.movieclub.app.stats.domain.WrappedCardContent
import cobresun.movieclub.app.theme.WrappedColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * The Wrapped story player, after the web's WrappedStory.vue. Cards sit on the
 * faces of a cube, stories-style: the current card faces the viewer and its
 * neighbours wait on the side faces. Swiping turns the cube with the finger;
 * tapping the left third goes back, anywhere else goes on.
 */

/** Largest the card may grow on a big screen before it stops reading as a story. */
private const val MAX_SCALE = 1.4f
private const val SETTLE_MS = 400
/** How far through a turn a release commits to it rather than springing back. */
private const val COMMIT_PROGRESS = 0.25f
/** How much a drag past the first or last card still turns, as a fraction. */
private const val EDGE_RESISTANCE = 0.2f
/** Side faces darken as they turn away, the way light falls on a real cube. */
private const val MAX_SHADE = 0.6f

private val SettleEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Overscroll that keeps turning the cube a little past the first or last card
 * and springs back on release, in place of the platform stretch or glow.
 */
private class CubeEdgeOverscroll : OverscrollEffect {
    /** Pixels the edge card is pulled; positive is towards the right. */
    var pull by mutableFloatStateOf(0f)
        private set

    override val isInProgress: Boolean get() = pull != 0f

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        var remaining = delta.x
        // A drag back the other way eases the pull before it scrolls.
        if (pull != 0f && remaining != 0f && sign(remaining) != sign(pull)) {
            val toRelease = abs(pull) / EDGE_RESISTANCE
            if (abs(remaining) >= toRelease) {
                remaining -= sign(remaining) * toRelease
                pull = 0f
            } else {
                pull += remaining * EDGE_RESISTANCE
                remaining = 0f
            }
        }
        val consumed = performScroll(Offset(remaining, delta.y))
        val leftover = remaining - consumed.x
        if (source == NestedScrollSource.UserInput && abs(leftover) > 0.5f) pull += leftover * EDGE_RESISTANCE
        return Offset(delta.x, consumed.y)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        performFling(velocity)
        if (pull != 0f) animate(pull, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) { value, _ -> pull = value }
    }
}

/** Lays [this] out at the card's fixed size and scales it to [scale], top-left anchored. */
private fun Modifier.scaledCard(scale: Float): Modifier = layout { measurable, _ ->
    val width = CARD_WIDTH.roundToPx()
    val height = CARD_HEIGHT.roundToPx()
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout((width * scale).roundToInt(), (height * scale).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** How far [page] is turned from facing the viewer: 0 facing, ±1 a full side face. */
private fun PagerState.turnOf(page: Int, pull: Float, width: Float): Float =
    (currentPage - page) + currentPageOffsetFraction - if (width > 0f) pull / width else 0f

/**
 * @param cardModifier applied to the current card at its fixed layout size,
 *   e.g. to capture it as an image.
 * @param shareButton sits between the previous and next chevrons.
 */
@Composable
internal fun WrappedStory(
    cards: List<WrappedCard>,
    clubName: String,
    year: Int,
    hapticFeedback: HapticFeedback,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { cards.size },
    cardModifier: Modifier = Modifier,
    shareButton: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val overscroll = remember { CubeEdgeOverscroll() }

    // A tick each time a turn lands on a new card.
    LaunchedEffect(pagerState, hapticFeedback) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().drop(1).collect { hapticFeedback.performTick() }
    }
    // The year opens with a flourish.
    LaunchedEffect(cards.firstOrNull()?.id) {
        if (cards.firstOrNull()?.content is WrappedCardContent.Intro) hapticFeedback.performSuccess()
    }

    fun turnTo(target: Int) {
        if (target !in cards.indices) return
        scope.launch {
            if (reducedMotion) pagerState.scrollToPage(target)
            else pagerState.animateScrollToPage(target, animationSpec = tween(SETTLE_MS, easing = SettleEasing))
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // Room left for the card once the progress bar and controls take theirs.
        val chromeHeight = 16.dp + 56.dp + 24.dp
        val scale = min(
            min(maxWidth / CARD_WIDTH, (maxHeight - chromeHeight) / CARD_HEIGHT),
            MAX_SCALE,
        ).coerceAtLeast(0.1f)
        val displayWidth = CARD_WIDTH * scale
        val displayHeight = CARD_HEIGHT * scale

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SegmentedProgress(
                count = cards.size,
                current = pagerState.currentPage,
                labels = cards.map { it.label },
                onJump = { scope.launch { pagerState.scrollToPage(it) } },
                modifier = Modifier.width(displayWidth),
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    key = { cards[it].id },
                    overscrollEffect = overscroll,
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = COMMIT_PROGRESS),
                    modifier = Modifier
                        .size(displayWidth, displayHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(cards.size, reducedMotion) {
                            detectTapGestures { position ->
                                val back = position.x < size.width / 3f
                                // A tap mid-turn continues from where that turn is headed.
                                val from = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
                                turnTo(from + if (back) -1 else 1)
                            }
                        },
                ) { page ->
                    // Content rises in the first time its card turns towards the viewer.
                    var seen by remember { mutableStateOf(false) }
                    val facing = page == pagerState.currentPage || page == pagerState.targetPage
                    if (facing && !seen) SideEffect { seen = true }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (reducedMotion) return@graphicsLayer
                                val turn = pagerState.turnOf(page, overscroll.pull, size.width)
                                translationX = overscroll.pull
                                cameraDistance = 12f * density
                                // Each face pivots on the seam it shares with the face beside it.
                                transformOrigin = TransformOrigin(if (turn > 0f) 1f else 0f, 0.5f)
                                rotationY = -90f * turn
                            }
                            .drawWithContent {
                                drawContent()
                                if (!reducedMotion) {
                                    val turn = pagerState.turnOf(page, overscroll.pull, size.width)
                                    drawRect(Color.Black, alpha = min(1f, abs(turn)) * MAX_SHADE)
                                }
                            },
                    ) {
                        CompositionLocalProvider(LocalCardReveal provides CardReveal(started = seen || facing, instant = reducedMotion)) {
                            WrappedCardFrame(
                                card = cards[page],
                                clubName = clubName,
                                year = year,
                                modifier = Modifier
                                    .scaledCard(scale)
                                    .then(if (page == pagerState.settledPage) cardModifier else Modifier)
                                    .semantics { contentDescription = "${page + 1} of ${cards.size}: ${cards[page].label}" },
                            )
                        }
                    }
                }
            }
            Row(Modifier.width(displayWidth).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                StoryChevron(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous card", pagerState.currentPage > 0) {
                    turnTo(pagerState.currentPage - 1)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { shareButton() }
                StoryChevron(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next card", pagerState.currentPage < cards.lastIndex) {
                    turnTo(pagerState.currentPage + 1)
                }
            }
        }
    }
}

@Composable
private fun StoryChevron(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = WrappedColors.Ink,
            disabledContentColor = WrappedColors.Ink.copy(alpha = 0.3f),
        ),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
    }
}

/** One tappable segment per card; segments up to the current one are lit. */
@Composable
private fun SegmentedProgress(
    count: Int,
    current: Int,
    labels: List<String>,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val color by animateColorAsState(
                if (index <= current) WrappedColors.Ink else WrappedColors.Ink.copy(alpha = 0.25f),
                tween(200),
            )
            Box(
                Modifier
                    .weight(1f)
                    .clickable(onClickLabel = "Card ${index + 1}: ${labels[index]}") { onJump(index) }
                    .padding(vertical = 6.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(color))
            }
        }
    }
}
