package cobresun.movieclub.app.awards.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.ScoredNomination
import cobresun.movieclub.app.awards.domain.scoreAward
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster

fun LazyListScope.resultsStep(state: AwardsState, onAction: (AwardsAction) -> Unit) {
    val season = state.loadedSeason ?: return
    items(season.categories, key = { "result-${it.title}" }) { category ->
        ResultCard(state, category, onAction, Modifier.padding(horizontal = 16.dp).animateItem())
    }
}

@Composable
private fun ResultCard(state: AwardsState, category: AwardCategory, onAction: (AwardsAction) -> Unit, modifier: Modifier) {
    val gold = MaterialTheme.colorScheme.tertiary
    val haptics = koinInject<HapticFeedback>()
    // Only celebrate a reveal that happens now, not winners already shown on open.
    var justRevealed by remember(category.title) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .border(1.dp, gold.copy(alpha = 0.25f), MaterialTheme.shapes.large),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, null, tint = gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(category.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        AnimatedContent(
            targetState = state.isRevealed(category),
            transitionSpec = {
                (fadeIn(tween(300, delayMillis = 100)) + expandVertically(tween(500, easing = RevealEasing), expandFrom = Alignment.Top)) togetherWith
                    fadeOut(tween(150))
            },
            label = "reveal",
        ) { revealed ->
            if (!revealed) {
                Envelope {
                    justRevealed = true
                    onAction(AwardsAction.Reveal(category.title))
                }
            } else {
                Revealed(state, category, celebrate = justRevealed, onCelebrate = { haptics.performSuccess() })
            }
        }
    }
}

@Composable
private fun Envelope(onReveal: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "And the award goes to…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onReveal,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) { Text("Reveal winner", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Revealed(state: AwardsState, category: AwardCategory, celebrate: Boolean, onCelebrate: () -> Unit) {
    val scored = scoreAward(category.nominations)
    val standings = scored.results
    if (standings.isEmpty()) {
        Text(
            "Nobody was nominated for this category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    val winners = standings.filter { it.winner }
    Column(Modifier.fillMaxWidth()) {
        if (winners.isNotEmpty()) WinnerHero(winners, celebrate, onCelebrate)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            "FULL RANKING",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            standings.forEach { StandingRow(it, scored.voters, state) }
        }
    }
}

/**
 * The winning poster(s). On a fresh reveal they spring up from slightly small
 * while a soft gold glow blooms behind them and settles, with a success haptic.
 */
@Composable
private fun WinnerHero(winners: List<ScoredNomination>, celebrate: Boolean, onCelebrate: () -> Unit) {
    val gold = MaterialTheme.colorScheme.tertiary
    val reducedMotion = rememberReducedMotion()
    val animate = celebrate && !reducedMotion
    val scale = remember { Animatable(if (animate) 0.82f else 1f) }
    val glow = remember { Animatable(if (animate) 0f else RestingGlow) }
    LaunchedEffect(Unit) {
        if (!celebrate) return@LaunchedEffect
        onCelebrate()
        if (!animate) return@LaunchedEffect
        launch { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        glow.animateTo(0.55f, tween(350, easing = RevealEasing))
        glow.animateTo(RestingGlow, tween(900))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(gold.copy(alpha = 0f), gold.copy(alpha = 0.10f)))),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (winners.size > 1) "IT'S A TIE" else "WINNER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = gold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                winners.forEach { standing ->
                    Column(
                        Modifier
                            .width(132.dp)
                            .semantics(mergeDescendants = true) {}
                            .clearAndSetSemantics { contentDescription = "${standing.nomination.title}, winner" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                                .drawBehind {
                                    val radius = size.maxDimension * 0.85f
                                    drawCircle(
                                        Brush.radialGradient(
                                            listOf(gold.copy(alpha = glow.value), gold.copy(alpha = 0f)),
                                            center = center,
                                            radius = radius,
                                        ),
                                        radius = radius,
                                    )
                                }
                                .border(2.dp, gold, MaterialTheme.shapes.small),
                        ) {
                            Poster(standing.nomination.posterUrl, standing.nomination.title, Modifier.width(132.dp))
                        }
                        Text(
                            standing.nomination.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        standing.nomination.releaseYear?.let {
                            Text("$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private const val RestingGlow = 0.18f
private val RevealEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

@Composable
private fun StandingRow(standing: ScoredNomination, voters: List<String>, state: AwardsState) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RankBadge(standing.place, highlighted = standing.winner)
        Poster(standing.nomination.posterUrl, standing.nomination.title, Modifier.width(40.dp), MaterialTheme.shapes.extraSmall)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    standing.nomination.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${standing.score} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Every voter's rank for this nominee, including members who have since left.
            if (voters.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    voters.forEach { voterId ->
                        val name = state.member(voterId)?.name ?: "Former member"
                        val rank = standing.nomination.ranking[voterId]
                        Row(
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                                .clearAndSetSemantics {
                                    contentDescription = "$name ranked it ${rank?.let { "#$it" } ?: "unranked"}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(name, state.member(voterId)?.imageUrl, size = 20.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(rank?.let { "#$it" } ?: "–", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
