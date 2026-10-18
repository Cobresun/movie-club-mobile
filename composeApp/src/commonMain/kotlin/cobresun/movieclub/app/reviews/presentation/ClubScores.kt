package cobresun.movieclub.app.reviews.presentation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import cobresun.movieclub.app.core.platform.HapticFeedback
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.outlined.Visibility
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.core.presentation.AppMotion
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** One tile in the club-scores grid: a member's score, or the club average (memberId null). */
private data class ScoreTile(val id: String, val memberId: String?, val name: String, val imageUrl: String?, val value: Double)

/**
 * Club scores as web's tile grid: members who scored, in club order, then the
 * average spanning both columns. Hidden scores show an eye until revealed, then
 * settle in one after another (80ms apart). [poppedAt] changes each time you
 * save a new score, which pops your own tile.
 */
@Composable
internal fun ClubScores(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
    revealed: Boolean,
    canReveal: Boolean,
    poppedAt: Int,
    onReveal: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Club scores")
        // Like web's reveal pill: collapses away once scores are revealed (or you score).
        AnimatedVisibility(visible = canReveal, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalButton(onClick = onReveal) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reveal club scores")
                }
            }
        }
        val tiles = buildList {
            members.forEach { m ->
                review.scores[m.id]?.let { add(ScoreTile("member_${m.id}", m.id, m.name.trim().substringBefore(' ').ifBlank { m.name }, m.imageUrl, it.value)) }
            }
            review.average?.let { add(ScoreTile("average", null, "Average", null, it)) }
        }
        if (tiles.isEmpty()) {
            Text(
                "No scores yet — be the first to rate it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        tiles.filter { it.memberId != null }.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (index, tile) ->
                    ScoreTileView(
                        tile = tile,
                        index = index,
                        hidden = ScoreReveal.isHidden(tile.memberId, currentUserId, revealed),
                        poppedAt = if (tile.memberId == currentUserId) poppedAt else 0,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        tiles.firstOrNull { it.memberId == null }?.let { avg ->
            ScoreTileView(
                tile = avg,
                index = tiles.size - 1,
                hidden = ScoreReveal.isHidden(null, currentUserId, revealed),
                poppedAt = 0,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScoreTileView(tile: ScoreTile, index: Int, hidden: Boolean, poppedAt: Int, modifier: Modifier = Modifier) {
    val haptics = koinInject<HapticFeedback>()
    val isAverage = tile.memberId == null
    val primary = MaterialTheme.colorScheme.primary

    // Reveal: hidden → shown settles from a slight shrink, staggered by position.
    val reveal = remember { Animatable(if (hidden) 0f else 1f) }
    LaunchedEffect(hidden) {
        if (hidden) reveal.snapTo(0f)
        else if (reveal.value < 1f) {
            delay(index * 80L)
            reveal.animateTo(1f, tween(500, easing = AppMotion.Emphasized))
        }
    }

    // Score pop (web `score-pop`): springy overshoot with a wobble and a fading primary ring.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(poppedAt) {
        if (poppedAt == 0) return@LaunchedEffect
        delay(250) // let the score sheet slide away first
        haptics.performSuccess()
        pop.snapTo(0f)
        pop.animateTo(1f, tween(700, easing = AppMotion.Emphasized))
    }
    // The pop is read only in the layer and draw lambdas, so the tile redraws for
    // its 700ms without recomposing every frame; only "popping or not" recomposes.
    val popping by remember { derivedStateOf { pop.value < 1f } }

    val shape = MaterialTheme.shapes.large
    Row(
        modifier
            .zIndex(if (popping) 1f else 0f)
            .graphicsLayer {
                val p = pop.value
                val popScale = keyframe(p, 1f, 1.12f, 0.97f, 1.05f, 1f)
                scaleX = popScale
                scaleY = popScale
                rotationZ = keyframe(p, 0f, -2.5f, 2f, -1f, 0f)
            }
            .drawBehind {
                val p = pop.value
                val ringAlpha = keyframe(p, 0f, 0.35f, 0.25f, 0.15f, 0f)
                val ringWidth = keyframe(p, 0f, 5f, 4f, 3f, 0f)
                if (ringAlpha > 0f) {
                    val w = ringWidth.dp.toPx()
                    drawRoundRect(
                        primary.copy(alpha = ringAlpha),
                        topLeft = Offset(-w, -w),
                        size = Size(size.width + 2 * w, size.height + 2 * w),
                        cornerRadius = CornerRadius(16.dp.toPx() + w),
                    )
                }
            }
            .clip(shape)
            .background(if (isAverage) primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (isAverage) Modifier.border(1.dp, primary.copy(alpha = 0.25f), shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isAverage) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
            }
        } else {
            Avatar(tile.name, tile.imageUrl, size = 28.dp)
        }
        Text(
            tile.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (hidden) {
            HiddenScoreIcon(Modifier.size(20.dp))
        } else {
            Text(
                ScoreScale.format(tile.value),
                style = if (isAverage) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (isAverage) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isAverage) primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    val s = 0.9f + 0.1f * reveal.value
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}

/** Piecewise-linear value at [t] across keyframes at 0, 25%, 50%, 70% and 100% (web `score-pop`). */
private fun keyframe(t: Float, v0: Float, v25: Float, v50: Float, v70: Float, v100: Float): Float {
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f
    return when {
        t <= 0.25f -> lerp(v0, v25, t / 0.25f)
        t <= 0.5f -> lerp(v25, v50, (t - 0.25f) / 0.25f)
        t <= 0.7f -> lerp(v50, v70, (t - 0.5f) / 0.2f)
        else -> lerp(v70, v100, (t - 0.7f) / 0.3f)
    }
}
