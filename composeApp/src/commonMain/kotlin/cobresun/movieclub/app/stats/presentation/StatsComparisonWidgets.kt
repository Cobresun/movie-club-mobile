package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.stats.domain.toFixed1

/* How members compare: taste similarity, outliers, consensus, and the club against TMDB. */

// --- Taste similarity --------------------------------------------------------

private enum class TasteScope { CLUB, YOU }
private enum class TasteMode { MOST, LEAST }

@Composable
internal fun TasteSimilarityCard(model: StatsModel, members: List<Member>, currentUserId: String?) {
    val club = model.tasteClub ?: return
    val yours = model.tasteYours
    val scopes = buildList {
        add(TasteScope.CLUB to "Club")
        if (yours?.mostSimilar != null) add(TasteScope.YOU to "You")
    }
    var pickedScope by rememberSaveable { mutableStateOf(TasteScope.CLUB) }
    val scope = scopes.pick(pickedScope)
    val similarity = if (scope == TasteScope.YOU) yours!! else club
    val modes = buildList {
        if (similarity.mostSimilar != null) add(TasteMode.MOST to "Most Similar")
        if (similarity.leastSimilar != null) add(TasteMode.LEAST to "Least Similar")
    }
    if (modes.isEmpty()) return
    var pickedMode by rememberSaveable { mutableStateOf(TasteMode.MOST) }
    val mode = modes.pick(pickedMode)
    val pair = (if (mode == TasteMode.MOST) similarity.mostSimilar else similarity.leastSimilar) ?: return
    val accent = if (mode == TasteMode.MOST) goodColor() else badColor()
    fun label(member: Member) = if (scope == TasteScope.YOU && member.id == currentUserId) "You" else member.firstName()

    StatCard(
        "Taste Similarity",
        subtitle = when (scope to mode) {
            TasteScope.CLUB to TasteMode.MOST -> "The pair whose scores line up the closest"
            TasteScope.CLUB to TasteMode.LEAST -> "The pair whose scores clash the hardest"
            TasteScope.YOU to TasteMode.MOST -> "The member whose scores line up closest with yours"
            else -> "The member whose scores clash hardest with yours"
        },
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (scopes.size > 1) SegmentedToggle(scopes, scope, { pickedScope = it }, Modifier.fillMaxWidth())
                SegmentedToggle(modes, mode, { pickedMode = it }, Modifier.fillMaxWidth())
            }
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            PairMember(pair.memberA, label(pair.memberA))
            Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${fmt(pair.similarityPercent)}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
                Text("similar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PairMember(pair.memberB, label(pair.memberB))
        }
        Text(
            buildString {
                append("Average score difference: ")
                append(fmt(pair.avgDifference))
                append(" points across ${pair.sharedCount} shared reviews")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 8.dp),
        )
        val highlights = if (mode == TasteMode.MOST) pair.bestAgreements else pair.worstAgreements
        if (highlights.isNotEmpty()) {
            SectionLabel(if (mode == TasteMode.MOST) "Top agreements" else "Biggest disagreements")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                highlights.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.work.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(
                            "${fmt(a.scoreA)} vs ${fmt(a.scoreB)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairMember(member: Member, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Avatar(member.name, member.imageUrl, size = 48.dp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// --- Outliers ----------------------------------------------------------------

private enum class OutlierMode { GUILTY, CURMUDGEON }

@Composable
internal fun MemberOutliersCard(model: StatsModel, members: List<Member>, clubType: ClubType) {
    val guilty = model.guiltyPleasures
    val curmudgeons = model.curmudgeons
    val options = buildList {
        if (guilty.isNotEmpty()) add(OutlierMode.GUILTY to "Guilty Pleasures")
        if (curmudgeons.isNotEmpty()) add(OutlierMode.CURMUDGEON to "Curmudgeons")
    }
    if (options.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(OutlierMode.GUILTY) }
    val mode = options.pick(picked)
    val entries = if (mode == OutlierMode.GUILTY) guilty else curmudgeons
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = entries.firstOrNull { it.member.id == memberId } ?: entries.first()
    val pluralNoun = nouns(clubType).second
    val isGuilty = mode == OutlierMode.GUILTY
    StatCard(
        if (isGuilty) "Guilty Pleasures" else "Club Curmudgeons",
        subtitle = if (isGuilty) "$pluralNoun where only one member loved it (2+ points above club average)"
        else "$pluralNoun where only one member hated it (2+ points below club average)",
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
                MemberFilterChips(entries.map { it.member }, selected.member.id, { memberId = it }, includeAll = false)
            }
        },
    ) {
        val color = if (isGuilty) goodColor() else badColor()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.works.forEach { w ->
                StatRow {
                    WorkInfo(w.work, "Club avg: ${w.clubAverage.toFixed1()}", Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        ValuePill(w.memberScore.toFixed1(), color)
                        Text(signed1(w.difference), style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp, end = 4.dp))
                    }
                }
            }
        }
    }
}

// --- Consensus ---------------------------------------------------------------

private enum class ConsensusMode { AGREED, DIVISIVE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConsensusCard(model: StatsModel, members: List<Member>) {
    val consensus = model.consensus
    if (consensus.mostAgreed.isEmpty()) return
    var mode by rememberSaveable { mutableStateOf(ConsensusMode.AGREED) }
    StatCard(
        "Club Consensus",
        subtitle = if (mode == ConsensusMode.AGREED) "Scores that landed closest together" else "Scores that split the room",
        controls = {
            SegmentedToggle(listOf(ConsensusMode.AGREED to "Most Agreed", ConsensusMode.DIVISIVE to "Most Divisive"), mode, { mode = it }, Modifier.fillMaxWidth())
        },
    ) {
        val list = if (mode == ConsensusMode.AGREED) consensus.mostAgreed else consensus.mostDivisive
        val color = if (mode == ConsensusMode.AGREED) goodColor() else badColor()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { e ->
                StatRow {
                    Poster(e.work.imageUrl, e.work.title, Modifier.width(40.dp), shape = RoundedCornerShape(6.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(e.work.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            e.scores.forEach { s ->
                                Text(
                                    "${s.member.firstName()}: ${fmt(s.score)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    ValuePill(e.average.toFixed1(), color)
                }
            }
        }
    }
}

// --- TMDB --------------------------------------------------------------------

private enum class DeviationMode { HIGHER, LOWER }

@Composable
internal fun TmdbDeviationCard(model: StatsModel) {
    val deviation = model.tmdbDeviation ?: return
    val options = buildList {
        if (deviation.clubRatedHigher.isNotEmpty()) add(DeviationMode.HIGHER to "We Liked More")
        if (deviation.clubRatedLower.isNotEmpty()) add(DeviationMode.LOWER to "We Liked Less")
    }
    if (options.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(DeviationMode.HIGHER) }
    val mode = options.pick(picked)
    val higher = mode == DeviationMode.HIGHER
    StatCard(
        "Club vs TMDB",
        subtitle = if (higher) "Movies your club rated above the TMDB average" else "Movies your club rated below the TMDB average",
        controls = { SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth()) },
    ) {
        val list = if (higher) deviation.clubRatedHigher else deviation.clubRatedLower
        val color = if (higher) goodColor() else badColor()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { e ->
                StatRow {
                    WorkInfo(e.work, "Club: ${fmt(e.clubScore)}  |  TMDB: ${fmt(e.externalScore)}", Modifier.weight(1f))
                    ValuePill((if (e.deviation > 0) "+" else "") + fmt(e.deviation), color)
                }
            }
        }
    }
}
