package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.stats.domain.PersonStat
import cobresun.movieclub.app.stats.domain.ReviewerTitle
import cobresun.movieclub.app.stats.domain.toFixed1
import cobresun.movieclub.app.theme.chartSeriesColor

/* Directors/actors/authors leaderboards and the reviewer leaderboard. */

// --- People ------------------------------------------------------------------

private enum class PeopleMode { DIRECTORS, ACTORS }

@Composable
internal fun PeopleCard(model: StatsModel, clubType: ClubType, actors: List<PersonStat>) {
    if (clubType == ClubType.BOOK) {
        val authors = model.authors
        if (authors.isEmpty()) return
        StatCard("Most Read Authors", subtitle = "Ranked by appearances in your club's reviews") {
            PersonLeaderboard(authors, itemNoun = "book")
        }
        return
    }
    val directors = model.directors
    val options = buildList {
        if (directors.isNotEmpty()) add(PeopleMode.DIRECTORS to "Directors")
        if (actors.isNotEmpty()) add(PeopleMode.ACTORS to "Actors")
    }
    if (options.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(PeopleMode.DIRECTORS) }
    val mode = options.pick(picked)
    StatCard("Filmmakers", subtitle = "Ranked by appearances in your club's reviews", controls = {
        SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
    }) {
        PersonLeaderboard(if (mode == PeopleMode.DIRECTORS) directors else actors, itemNoun = "film")
    }
}

/** web PersonLeaderboard: ranked people, tap a row to list the works they appear in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonLeaderboard(entries: List<PersonStat>, itemNoun: String) {
    var expanded by remember(entries) { mutableStateOf(emptySet<String>()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEachIndexed { index, entry ->
            val isOpen = entry.name in expanded
            val color = when {
                entry.averageScore >= 7.5 -> goodColor()
                entry.averageScore >= 5 -> MaterialTheme.colorScheme.primary
                else -> badColor()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(role = Role.Button, onClickLabel = if (isOpen) "Hide titles" else "Show titles") {
                        expanded = if (isOpen) expanded - entry.name else expanded + entry.name
                    }
                    .semantics { stateDescription = if (isOpen) "Expanded" else "Collapsed" }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    RankedAvatar(entry, index)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(plural(entry.workCount, itemNoun), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.averageScore.toFixed1(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
                            Icon(
                                if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        GrowingBar((entry.averageScore / 10).toFloat(), color.copy(alpha = 0.7f), index, Modifier.fillMaxWidth())
                    }
                }
                AnimatedVisibility(isOpen, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    FlowRow(
                        Modifier.padding(start = 54.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        entry.works.forEach { work ->
                            Text(
                                work,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 160.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Gold, silver and bronze for the podium; muted for the rest. */
@Composable
private fun rankColor(index: Int): Color = when (index) {
    0 -> MaterialTheme.colorScheme.tertiary
    1 -> MaterialTheme.colorScheme.onSurfaceVariant
    2 -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun RankedAvatar(entry: PersonStat, index: Int) {
    val color = rankColor(index)
    Box(Modifier.size(40.dp)) {
        if (entry.imageUrl != null) {
            Avatar(entry.name, entry.imageUrl, size = 40.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
            }
        } else {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text("${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

// --- Reviewers ---------------------------------------------------------------

@Composable
internal fun ReviewerLeaderboardCard(model: StatsModel, members: List<Member>) {
    val entries = model.leaderboard
    if (entries.isEmpty()) return
    StatCard("Reviewer Stats", subtitle = "Average score per member — chart colors match") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.forEachIndexed { index, e ->
                val color = chartSeriesColor(maxOf(0, members.indexOf(e.member)))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(e.member.name, e.member.imageUrl, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                Text(
                                    e.member.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                e.title?.let { title ->
                                    val badge = if (title == ReviewerTitle.SOFTIE) goodColor() else badColor()
                                    Text(
                                        if (title == ReviewerTitle.SOFTIE) "The Softie" else "The Hater",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badge,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(badge.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(plural(e.reviewCount, "review"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(e.averageScore.toFixed1(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    GrowingBar((e.averageScore / 10).toFloat(), color, index, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
