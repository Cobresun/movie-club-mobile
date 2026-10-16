package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.formatRuntime
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.stats.domain.CategoryCount
import cobresun.movieclub.app.stats.domain.CategoryStat
import cobresun.movieclub.app.stats.domain.CreditedPerson
import cobresun.movieclub.app.stats.domain.PairSimilarity
import cobresun.movieclub.app.stats.domain.PersonStat
import cobresun.movieclub.app.stats.domain.ReviewerTitle
import cobresun.movieclub.app.stats.domain.StatsSummary
import cobresun.movieclub.app.stats.domain.WorkStat
import cobresun.movieclub.app.stats.domain.computeClubRecords
import cobresun.movieclub.app.stats.domain.computeConsensus
import cobresun.movieclub.app.stats.domain.computeCumulativeCounts
import cobresun.movieclub.app.stats.domain.computeCurmudgeons
import cobresun.movieclub.app.stats.domain.computeGenreCounts
import cobresun.movieclub.app.stats.domain.computeGenreStats
import cobresun.movieclub.app.stats.domain.computeGuiltyPleasures
import cobresun.movieclub.app.stats.domain.computeLeaderboard
import cobresun.movieclub.app.stats.domain.computeMonthlyActivity
import cobresun.movieclub.app.stats.domain.computeScoreDistribution
import cobresun.movieclub.app.stats.domain.computeScoreSpreadTrend
import cobresun.movieclub.app.stats.domain.computeScoreTrend
import cobresun.movieclub.app.stats.domain.computeSubjectCounts
import cobresun.movieclub.app.stats.domain.computeSubjectStats
import cobresun.movieclub.app.stats.domain.computeTasteSimilarity
import cobresun.movieclub.app.stats.domain.computeTmdbDeviation
import cobresun.movieclub.app.stats.domain.computeTopActors
import cobresun.movieclub.app.stats.domain.computeTopAuthors
import cobresun.movieclub.app.stats.domain.computeTopDirectors
import cobresun.movieclub.app.theme.chartSeriesColor

private fun fmt(v: Double) = ScoreScale.format(v)
private fun signed(v: Double) = (if (v > 0) "+" else "") + fmt(v)
private fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"

/** Positive / negative accents (green, red) taken from the theme. */
@Composable private fun goodColor() = MaterialTheme.colorScheme.secondary
@Composable private fun badColor() = MaterialTheme.colorScheme.error
@Composable private fun scoreColor() = MaterialTheme.colorScheme.tertiary

private fun Member.firstName() = name.trim().substringBefore(' ').ifBlank { name }

// --- Summary -----------------------------------------------------------------

@Composable
fun SummaryTiles(summary: StatsSummary, clubType: ClubType) {
    val isMovie = clubType == ClubType.MOVIE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryTile(
            icon = if (isMovie) Icons.Outlined.Movie else Icons.AutoMirrored.Outlined.MenuBook,
            value = summary.workCount.toString(),
            label = if (isMovie) "watched" else "read",
            modifier = Modifier.weight(1f),
        )
        when {
            isMovie && summary.runtimeMinutes > 0 -> {
                val hours = summary.runtimeMinutes / 60
                SummaryTile(
                    icon = Icons.Outlined.Schedule,
                    value = if (hours >= 24) "${hours}h" else formatRuntime(summary.runtimeMinutes),
                    label = if (hours >= 48) "${(hours + 12) / 24} days" else "watch time",
                    modifier = Modifier.weight(1f),
                )
            }
            !isMovie && summary.pageCount > 0 -> SummaryTile(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                value = formatThousands(summary.pageCount),
                label = "pages",
                modifier = Modifier.weight(1f),
            )
        }
        summary.clubAverage?.let {
            SummaryTile(Icons.Outlined.StarOutline, fmt(it), "avg score", Modifier.weight(1f), accent = scoreColor())
        }
    }
}

internal fun formatThousands(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
private fun SummaryTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// --- Records -----------------------------------------------------------------

@Composable
fun ClubRecordsCard(reviews: List<Review>) {
    val records = remember(reviews) { computeClubRecords(reviews) } ?: return
    StatCard("Club Records") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecordTile("Highest", records.highest, fmt(records.highest.value), goodColor(), Modifier.weight(1f))
            RecordTile("Lowest", records.lowest, fmt(records.lowest.value), badColor(), Modifier.weight(1f))
            records.mostDivisive?.let {
                RecordTile("Divisive", it, "±" + fmt(it.value), scoreColor(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecordTile(label: String, stat: WorkStat, value: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Poster(stat.work.imageUrl, stat.work.title, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            ValuePill(value, color, Modifier.align(Alignment.BottomStart).padding(6.dp).background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.85f), CircleShape))
        }
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        Text(stat.work.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// --- Scores ------------------------------------------------------------------

private enum class ScoreMode { DISTRIBUTION, TREND, AGREEMENT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoresCard(reviews: List<Review>, members: List<Member>) {
    val distribution = remember(reviews, members) { computeScoreDistribution(reviews, members) }
    val trend = remember(reviews, members) { computeScoreTrend(reviews, members) }
    val spread = remember(reviews) { computeScoreSpreadTrend(reviews) }
    val options = buildList {
        add(ScoreMode.DISTRIBUTION to "Distribution")
        if (trend.isNotEmpty()) add(ScoreMode.TREND to "Trend")
        if (spread.isNotEmpty()) add(ScoreMode.AGREEMENT to "Agreement")
    }
    var picked by rememberSaveable { mutableStateOf(ScoreMode.DISTRIBUTION) }
    val mode = if (options.any { it.first == picked }) picked else ScoreMode.DISTRIBUTION
    val subtitle = when (mode) {
        ScoreMode.DISTRIBUTION -> "How often each score is given"
        ScoreMode.TREND -> "Each member's rolling average over time"
        ScoreMode.AGREEMENT -> "Rolling score spread · lower means more agreement"
    }
    StatCard("Scores", subtitle = subtitle, controls = {
        SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
    }) {
        AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "scores") { m ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (m) {
                    ScoreMode.DISTRIBUTION -> StackedScoreBars(
                        members.mapIndexed { i, mem -> chartSeriesColor(i) to (distribution.binsByMember[mem.id] ?: List(11) { 0 }) },
                    )
                    ScoreMode.TREND -> {
                        val series = members.mapIndexedNotNull { i, mem -> trend[mem.id]?.let { chartSeriesColor(i) to it } }
                        val lo = series.flatMap { it.second }.minOfOrNull { it.value }?.let { maxOf(0.0, kotlin.math.floor(it) - 1) } ?: 0.0
                        TimeLines(series, yMin = lo, yMax = 10.0)
                    }
                    ScoreMode.AGREEMENT -> TimeLines(listOf(MaterialTheme.colorScheme.secondary to spread))
                }
                if (m != ScoreMode.AGREEMENT && members.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEachIndexed { i, mem ->
                            if (m == ScoreMode.DISTRIBUTION || trend.containsKey(mem.id)) LegendItem(chartSeriesColor(i), mem.firstName())
                        }
                    }
                }
            }
        }
    }
}

// --- Activity ----------------------------------------------------------------

private enum class ActivityMode { MONTHLY, TOTAL }

@Composable
fun ActivityCard(reviews: List<Review>, clubType: ClubType) {
    val monthly = remember(reviews) { computeMonthlyActivity(reviews) }
    val cumulative = remember(reviews) { computeCumulativeCounts(reviews) }
    if (monthly.isEmpty()) return
    var mode by rememberSaveable { mutableStateOf(ActivityMode.MONTHLY) }
        val busiest = monthly.maxBy { it.count }
    StatCard(
        "Activity",
        subtitle = "Busiest month: ${monthLabel(busiest.month, busiest.year)} · ${if (clubType == ClubType.MOVIE) plural(busiest.count, "movie") + " watched" else plural(busiest.count, "book") + " read"}",
        controls = {
            SegmentedToggle(listOf(ActivityMode.MONTHLY to "Per month", ActivityMode.TOTAL to "Running total"), mode, { mode = it }, Modifier.fillMaxWidth())
        },
    ) {
        AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "activity") { m ->
            when (m) {
                ActivityMode.MONTHLY -> MonthlyBars(
                    counts = monthly.map { it.count },
                    labels = monthly.map { monthLabel(it.month, it.year) },
                    color = MaterialTheme.colorScheme.primary,
                )
                ActivityMode.TOTAL -> TimeLines(listOf(MaterialTheme.colorScheme.primary to cumulative))
            }
        }
    }
}

// --- Genres / subjects -------------------------------------------------------

private enum class CategoryMode { LOVED, POPULAR }

@Composable
fun GenresCard(reviews: List<Review>, members: List<Member>, clubType: ClubType) {
    val isMovie = clubType == ClubType.MOVIE
    var mode by rememberSaveable { mutableStateOf(CategoryMode.LOVED) }
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    val counts = remember(reviews) { if (isMovie) computeGenreCounts(reviews) else computeSubjectCounts(reviews) }
    if (counts.isEmpty()) return
    val title = if (isMovie) "Genres" else "Subjects"
    StatCard(
        title,
        subtitle = if (mode == CategoryMode.LOVED) "Average score by ${title.lowercase().dropLast(1)} (2+ ${if (isMovie) "movies" else "books"})"
        else if (isMovie) "What the club watches most" else "What the club reads most",
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedToggle(listOf(CategoryMode.LOVED to "Best rated", CategoryMode.POPULAR to "Most ${if (isMovie) "watched" else "read"}"), mode, { mode = it }, Modifier.fillMaxWidth())
                if (mode == CategoryMode.LOVED && members.size > 1) MemberFilterChips(members, memberId, { memberId = it })
            }
        },
    ) {
        when (mode) {
            CategoryMode.POPULAR -> CountBars(counts)
            CategoryMode.LOVED -> {
                if (isMovie) {
                    val stats = remember(reviews, memberId) { computeGenreStats(reviews, memberId) }
                    if (stats.mostLoved.isEmpty()) CardHint("Not enough scores yet: a genre needs two scored movies.")
                    else {
                        SectionLabel("Most loved", goodColor())
                        ScoreBars(stats.mostLoved, goodColor())
                        val least = stats.leastLoved.filter { it !in stats.mostLoved }
                        if (least.isNotEmpty()) {
                            SectionLabel("Least loved", badColor())
                            ScoreBars(least, badColor())
                        }
                    }
                } else {
                    val stats = remember(reviews, memberId) { computeSubjectStats(reviews, memberId) }
                    if (stats.isEmpty()) CardHint("Not enough scores yet: a subject needs two scored books.")
                    else ScoreBars(stats, goodColor())
                }
            }
        }
    }
}

@Composable
private fun ScoreBars(stats: List<CategoryStat>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { RankedBar(it.name, fmt(it.averageScore), (it.averageScore / 10).toFloat(), color, supporting = plural(it.count, "review")) }
    }
}

@Composable
private fun CountBars(counts: List<CategoryCount>) {
    val max = counts.maxOf { it.count }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        counts.forEach { RankedBar(it.name, it.count.toString(), it.count.toFloat() / max, MaterialTheme.colorScheme.primary) }
    }
}

// --- People ------------------------------------------------------------------

private enum class PeopleMode { DIRECTORS, ACTORS }

@Composable
fun PeopleCard(reviews: List<Review>, clubType: ClubType, cast: Map<String, List<CreditedPerson>>?) {
    if (clubType == ClubType.BOOK) {
        val authors = remember(reviews) { computeTopAuthors(reviews) }
        if (authors.isEmpty()) return
        StatCard("Authors", subtitle = "Most-read authors and how the club rated them") { PeopleList(authors) }
        return
    }
    val directors = remember(reviews) { computeTopDirectors(reviews) }
    val actors = remember(reviews, cast) { cast?.let { computeTopActors(reviews, it) }.orEmpty() }
    if (directors.isEmpty() && actors.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(PeopleMode.DIRECTORS) }
    val options = buildList {
        if (directors.isNotEmpty()) add(PeopleMode.DIRECTORS to "Directors")
        if (actors.isNotEmpty()) add(PeopleMode.ACTORS to "Actors")
    }
    val mode = if (options.any { it.first == picked }) picked else options.first().first
    StatCard("People", subtitle = "Most-watched and how the club rated them", controls = {
        SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
    }) {
        PeopleList(if (mode == PeopleMode.DIRECTORS) directors else actors)
    }
}

@Composable
private fun PeopleList(people: List<PersonStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        people.forEachIndexed { i, p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${i + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(14.dp),
                    textAlign = TextAlign.Center,
                )
                Avatar(p.name, p.imageUrl, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${plural(p.workCount, "title")} · ${p.works.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ValuePill(fmt(p.averageScore), scoreColor())
            }
        }
    }
}

// --- Reviewers ---------------------------------------------------------------

@Composable
fun ReviewerLeaderboardCard(reviews: List<Review>, members: List<Member>) {
    val entries = remember(reviews, members) { computeLeaderboard(reviews, members) }
    if (entries.size < 2) return
    StatCard("Reviewers", subtitle = "Average score each member gives") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            entries.forEach { e ->
                val color = chartSeriesColor(members.indexOf(e.member))
                RankedBar(
                    label = e.member.name,
                    value = fmt(e.averageScore),
                    fraction = (e.averageScore / 10).toFloat(),
                    color = color,
                    supporting = buildString {
                        append(plural(e.reviewCount, "review"))
                        when (e.title) {
                            ReviewerTitle.SOFTIE -> append(" · The Softie")
                            ReviewerTitle.HATER -> append(" · The Hater")
                            null -> {}
                        }
                    },
                    leading = { Avatar(e.member.name, e.member.imageUrl, size = 36.dp) },
                )
            }
        }
    }
}

// --- Taste similarity --------------------------------------------------------

@Composable
fun TasteSimilarityCard(reviews: List<Review>, members: List<Member>, currentUserId: String?) {
    val isMember = members.any { it.id == currentUserId }
    var scope by rememberSaveable { mutableStateOf<String?>(null) }
    val scoped = scope.takeIf { s -> members.any { it.id == s } }
    val result = remember(reviews, members, scoped) { computeTasteSimilarity(reviews, members, scoped) }
    val everyone = remember(reviews, members) { computeTasteSimilarity(reviews, members) }
    if (everyone.mostSimilar == null) return
    StatCard(
        "Taste Similarity",
        subtitle = "How closely members' scores line up (3+ shared reviews)",
        controls = if (members.size > 2) {
            {
                // Put the viewer first, labelled "You".
                val ordered = members.sortedByDescending { it.id == currentUserId }
                MemberFilterChips(ordered, scoped, { scope = it }, labelFor = { if (isMember && it.id == currentUserId) "You" else it.firstName() })
            }
        } else null,
    ) {
        val most = result.mostSimilar
        val least = result.leastSimilar
        if (most == null) {
            CardHint("Not enough shared reviews yet for this member.")
        } else {
            PairRow("Most alike", most, goodColor(), currentUserId)
            if (least != null && least != most) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                PairRow("Least alike", least, badColor(), currentUserId)
            }
        }
    }
}

@Composable
private fun PairRow(label: String, pair: PairSimilarity, color: Color, currentUserId: String?) {
    fun Member.short() = if (id == currentUserId) "You" else firstName()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(label, color)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(62.dp)) {
                Avatar(pair.memberA.name, pair.memberA.imageUrl, size = 38.dp)
                Avatar(pair.memberB.name, pair.memberB.imageUrl, size = 38.dp, modifier = Modifier.align(Alignment.CenterEnd))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${pair.memberA.short()} & ${pair.memberB.short()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${plural(pair.sharedCount, "shared review")} · ${fmt(pair.avgDifference)} pts apart on average",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${pair.similarityPercent.toInt()}%", style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
        val examples = if (color == goodColor()) pair.bestAgreements.take(2) else pair.worstAgreements.take(2)
        examples.forEach { a ->
            WorkStatRow(a.work, "${pair.memberA.short()} ${fmt(a.scoreA)} · ${pair.memberB.short()} ${fmt(a.scoreB)}")
        }
    }
}

// --- Outliers ----------------------------------------------------------------

private enum class OutlierMode { GUILTY, CURMUDGEON }

@Composable
fun MemberOutliersCard(reviews: List<Review>, members: List<Member>, clubType: ClubType) {
    val guilty = remember(reviews, members) { computeGuiltyPleasures(reviews, members) }
    val curmudgeons = remember(reviews, members) { computeCurmudgeons(reviews, members) }
    if (guilty.isEmpty() && curmudgeons.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(OutlierMode.GUILTY) }
    val options = buildList {
        if (guilty.isNotEmpty()) add(OutlierMode.GUILTY to "Guilty pleasures")
        if (curmudgeons.isNotEmpty()) add(OutlierMode.CURMUDGEON to "Curmudgeons")
    }
    val mode = if (options.any { it.first == picked }) picked else options.first().first
    val entries = if (mode == OutlierMode.GUILTY) guilty else curmudgeons
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = entries.firstOrNull { it.member.id == memberId } ?: entries.first()
    val noun = if (clubType == ClubType.MOVIE) "movies" else "books"
    StatCard(
        if (mode == OutlierMode.GUILTY) "Guilty Pleasures" else "Club Curmudgeons",
        subtitle = if (mode == OutlierMode.GUILTY) "${noun.replaceFirstChar { it.uppercase() }} one member alone loved, 2+ above the club"
        else "${noun.replaceFirstChar { it.uppercase() }} one member alone disliked, 2+ below the club",
        controls = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
                MemberFilterChips(entries.map { it.member }, selected.member.id, { memberId = it }, includeAll = false, labelFor = { "${it.firstName()} · ${entries.first { e -> e.member.id == it.id }.works.size}" })
            }
        },
    ) {
        val color = if (mode == OutlierMode.GUILTY) goodColor() else badColor()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            selected.works.forEach { w ->
                WorkStatRow(w.work, "${selected.member.firstName()} ${fmt(w.memberScore)} · club ${fmt(w.clubAverage)}") {
                    ValuePill(signed(w.difference), color)
                }
            }
        }
    }
}

// --- Consensus ---------------------------------------------------------------

private enum class ConsensusMode { AGREED, DIVISIVE }

@Composable
fun ConsensusCard(reviews: List<Review>, members: List<Member>) {
    val consensus = remember(reviews, members) { computeConsensus(reviews, members) }
    if (consensus.mostAgreed.isEmpty()) return
    var mode by rememberSaveable { mutableStateOf(ConsensusMode.AGREED) }
    StatCard("Club Consensus", subtitle = "Where scores were closest together and furthest apart", controls = {
        SegmentedToggle(listOf(ConsensusMode.AGREED to "Agreed", ConsensusMode.DIVISIVE to "Divisive"), mode, { mode = it }, Modifier.fillMaxWidth())
    }) {
        val list = if (mode == ConsensusMode.AGREED) consensus.mostAgreed else consensus.mostDivisive
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            list.forEach { e ->
                WorkStatRow(e.work, e.scores.joinToString(" · ") { "${it.member.firstName()} ${fmt(it.score)}" }) {
                    Column(horizontalAlignment = Alignment.End) {
                        ValuePill(fmt(e.average), scoreColor())
                        Text("±${fmt(e.stdDev)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp, end = 4.dp))
                    }
                }
            }
        }
    }
}

// --- TMDB --------------------------------------------------------------------

private enum class DeviationMode { HIGHER, LOWER }

@Composable
fun TmdbDeviationCard(reviews: List<Review>) {
    val deviation = remember(reviews) { computeTmdbDeviation(reviews) }
    if (deviation.clubRatedHigher.isEmpty() && deviation.clubRatedLower.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(DeviationMode.HIGHER) }
    val options = buildList {
        if (deviation.clubRatedHigher.isNotEmpty()) add(DeviationMode.HIGHER to "Club liked more")
        if (deviation.clubRatedLower.isNotEmpty()) add(DeviationMode.LOWER to "Club liked less")
    }
    val mode = if (options.any { it.first == picked }) picked else options.first().first
    StatCard("vs. TMDB", subtitle = "Where the club disagrees with TMDB's audience", controls = {
        SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth())
    }) {
        val list = if (mode == DeviationMode.HIGHER) deviation.clubRatedHigher else deviation.clubRatedLower
        val color = if (mode == DeviationMode.HIGHER) goodColor() else badColor()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            list.forEach { e ->
                WorkStatRow(e.work, "Club ${fmt(e.clubScore)} · TMDB ${fmt(e.externalScore)}") {
                    ValuePill(signed(e.deviation), color)
                }
            }
        }
    }
}

/** Shown above the widgets while there's too little data for most of them to say much. */
@Composable
fun FewReviewsHint(count: Int, clubType: ClubType) {
    val noun = if (clubType == ClubType.MOVIE) "movie" else "book"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.StarOutline, null, tint = MaterialTheme.colorScheme.secondary)
        Text(
            "Just getting started: ${plural(count, "scored $noun")} so far. Records, taste matches and trends fill in as the club scores more.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

