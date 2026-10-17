package cobresun.movieclub.app.awards.presentation

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsStep
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
            transitionSpec = { (fadeIn(tween(400)) + expandVertically(tween(400))) togetherWith fadeOut(tween(150)) },
            label = "reveal",
        ) { revealed ->
            if (!revealed) {
                Envelope { onAction(AwardsAction.Reveal(category.title)) }
            } else {
                Revealed(state, category)
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
private fun Revealed(state: AwardsState, category: AwardCategory) {
    val standings = scoreAward(category.nominations).results
    if (standings.isEmpty()) {
        Text(
            "No nominees in this category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    val winners = standings.filter { it.winner }
    Column(Modifier.fillMaxWidth()) {
        if (winners.isNotEmpty()) WinnerHero(winners)
        val rest = standings.filterNot { it.winner }
        if (rest.isNotEmpty() || state.members.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                "FULL RANKING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                standings.forEach { StandingRow(it, state) }
            }
        }
    }
}

@Composable
private fun WinnerHero(winners: List<ScoredNomination>) {
    val gold = MaterialTheme.colorScheme.tertiary
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
            Text(
                if (winners.size > 1) "IT'S A TIE" else "WINNER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                winners.forEach { standing ->
                    Column(Modifier.width(132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.border(2.dp, gold, MaterialTheme.shapes.small)) {
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

@Composable
private fun StandingRow(standing: ScoredNomination, state: AwardsState) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RankBadge(standing.place)
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
            // Each voter's rank for this nominee.
            val votes = standing.nomination.ranking.entries.sortedBy { it.value }
            if (votes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    votes.forEach { (voterId, rank) ->
                        val member = state.member(voterId)
                        Row(
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(member?.name ?: "?", member?.imageUrl, size = 20.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("#$rank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
