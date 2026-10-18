package cobresun.movieclub.app.reviews.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.VisibilityOff
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cobresun.movieclub.app.core.domain.Work
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ScoreScale

/* Review grid cards and the score chips and badges they (and the detail sheet) use. */

@Composable
internal fun ReviewCard(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
    /** False hides everyone else's scores and the average (see ScoreReveal). */
    revealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val work = review.work
    val needsMyScore = currentUserId != null && review.scoreOf(currentUserId) == null
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Box {
            Poster(work.imageUrl, work.title, Modifier.fillMaxWidth())
            review.average?.let { avg ->
                // Cards never reveal on tap (same as web): that's the detail sheet's job.
                ScoreBadge(
                    value = avg.takeUnless { ScoreReveal.isHidden(null, currentUserId, revealed) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
            if (needsMyScore) {
                Text(
                    "Rate it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(work.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        work.addedDate?.let {
            Text(
                it.formatShort(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val scored = members.mapNotNull { m -> review.scores[m.id]?.let { m to it.value } }
        if (scored.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                scored.forEach { (member, value) ->
                    MemberScoreChip(
                        member,
                        value.takeUnless { ScoreReveal.isHidden(member.id, currentUserId, revealed) },
                        isMe = member.id == currentUserId,
                    )
                }
            }
        }
    }
}

/**
 * A work still on its way onto the reviews list: there's nothing to score or
 * open until the refetch replaces it, so it isn't selectable.
 */
@Composable
internal fun PendingReviewCard(work: Work, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "Adding ${work.title}" }) {
        Box(contentAlignment = Alignment.Center) {
            Poster(work.imageUrl, work.title, Modifier.fillMaxWidth().alpha(0.5f))
            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(work.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Adding…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MemberScoreChip(member: Member, value: Double?, isMe: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (isMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Avatar(member.name, member.imageUrl, size = 18.dp)
        if (value == null) {
            HiddenScoreIcon(Modifier.size(14.dp))
        } else {
            Text(
                ScoreScale.format(value),
                style = MaterialTheme.typography.labelMedium,
                color = if (isMe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Placeholder for a score kept hidden until you score the work or reveal it. */
@Composable
fun HiddenScoreIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Outlined.VisibilityOff,
        contentDescription = "Hidden score",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Gold star + club average (null while hidden), legible over any poster. */
@Composable
fun ScoreBadge(value: Double?, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
            .padding(horizontal = if (large) 10.dp else 7.dp, vertical = if (large) 6.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(if (large) 20.dp else 14.dp),
        )
        if (value == null) {
            HiddenScoreIcon(Modifier.size(if (large) 20.dp else 14.dp))
        } else {
            Text(
                ScoreScale.format(value),
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
