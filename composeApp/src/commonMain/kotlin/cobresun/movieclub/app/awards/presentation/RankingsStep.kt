package cobresun.movieclub.app.awards.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.isRankedBy
import cobresun.movieclub.app.awards.presentation.components.DragHandle
import cobresun.movieclub.app.awards.presentation.components.Pill
import cobresun.movieclub.app.awards.presentation.components.ReorderableColumn
import cobresun.movieclub.app.awards.presentation.components.SectionCard
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster

fun LazyListScope.rankingsStep(state: AwardsState, onAction: (AwardsAction) -> Unit) {
    val season = state.loadedSeason ?: return
    item(key = "rankings-intro") {
        StepIntro("Drag each category's nominees into your order, best at the top, then submit. You can resubmit until results open.")
    }
    items(season.categories, key = { "rank-${it.title}" }) { category ->
        RankingCard(state, category, onAction, Modifier.padding(horizontal = 16.dp).animateItem())
    }
}

@Composable
private fun RankingCard(
    state: AwardsState,
    category: AwardCategory,
    onAction: (AwardsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val me = state.currentUserId.orEmpty()
    val submitted = category.nominations.isNotEmpty() && category.isRankedBy(me)
    val edited = category.title in state.rankingDrafts
    val submitting = category.title in state.submittingRankings
    SectionCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (state.members.isNotEmpty() && category.nominations.isNotEmpty()) {
                    Text(
                        "${state.memberIds.count { category.isRankedBy(it) }} of ${state.members.size} members ranked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                edited -> Pill("Unsaved")
                submitted -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Submitted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        if (category.nominations.isEmpty()) {
            Text(
                "Nobody nominated anything here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        ReorderableColumn(
            items = state.rankingFor(category),
            key = { it.movieId },
            onMove = { from, to -> onAction(AwardsAction.MoveNominee(category.title, from, to)) },
        ) { nomination ->
            RankRow(nomination, index + 1, isDragging, state, handleModifier)
        }
        val label = when {
            submitting -> "Saving…"
            submitted && !edited -> "Ranking saved"
            submitted -> "Update ranking"
            else -> "Submit ranking"
        }
        if (!submitted || edited) {
            Button(
                onClick = { onAction(AwardsAction.SubmitRanking(category.title)) },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(label)
            }
        } else {
            FilledTonalButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}

@Composable
private fun RankRow(
    nomination: Nomination,
    rank: Int,
    dragging: Boolean,
    state: AwardsState,
    handleModifier: Modifier,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.shapes.medium,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RankBadge(rank)
        Poster(nomination.posterUrl, nomination.title, Modifier.width(44.dp), MaterialTheme.shapes.extraSmall)
        Column(Modifier.weight(1f)) {
            Text(nomination.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), modifier = Modifier.padding(top = 4.dp)) {
                nomination.nominatedBy.take(5).forEach { id ->
                    val member = state.member(id)
                    Avatar(member?.name ?: "?", member?.imageUrl, size = 20.dp)
                }
            }
        }
        DragHandle(handleModifier.padding(4.dp), contentDescription = "Reorder ${nomination.title}")
    }
}

@Composable
fun RankBadge(rank: Int) {
    val first = rank == 1
    Box(
        Modifier
            .size(28.dp)
            .background(
                if (first) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHighest,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (first) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
