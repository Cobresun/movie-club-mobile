package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.stats.domain.CategoryCount
import cobresun.movieclub.app.stats.domain.CategoryStat
import cobresun.movieclub.app.stats.domain.CreditedPerson
import cobresun.movieclub.app.stats.domain.PersonStat
import cobresun.movieclub.app.stats.domain.ReviewerTitle
import cobresun.movieclub.app.stats.domain.StatsSummary
import cobresun.movieclub.app.stats.domain.WorkStat
import cobresun.movieclub.app.stats.domain.computeClubRecords
import cobresun.movieclub.app.stats.domain.computeConsensus
import cobresun.movieclub.app.stats.domain.computeCumulativeCounts
import cobresun.movieclub.app.stats.domain.computeCurmudgeons
import cobresun.movieclub.app.stats.domain.computeGenreStats
import cobresun.movieclub.app.stats.domain.computeGenreWatchCounts
import cobresun.movieclub.app.stats.domain.computeGuiltyPleasures
import cobresun.movieclub.app.stats.domain.computeHighestRatedByYear
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
import cobresun.movieclub.app.stats.domain.toFixed1
import cobresun.movieclub.app.theme.chartSeriesColor

/*
 * The statistics widgets, in the web's `statisticsWidgets.ts` order and with
 * its copy. Each widget hides itself when it has nothing to show.
 */

private fun fmt(v: Double) = ScoreScale.format(v)
private fun signed1(v: Double) = (if (v > 0) "+" else "") + v.toFixed1()
private fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"

/** Positive / negative accents taken from the theme. */
@Composable private fun goodColor() = MaterialTheme.colorScheme.secondary
@Composable private fun badColor() = MaterialTheme.colorScheme.error
@Composable private fun scoreColor() = MaterialTheme.colorScheme.tertiary

private fun Member.firstName() = name.trim().substringBefore(' ').ifBlank { name }

private fun nouns(type: ClubType) = if (type == ClubType.BOOK) "book" to "Books" else "movie" to "Movies"

/** Picks [picked] when it's still offered, else the first option (web's tab fallback). */
private fun <T> List<Pair<T, String>>.pick(picked: T): T = if (any { it.first == picked }) picked else first().first

// --- Summary -----------------------------------------------------------------

@Composable
fun SummaryTiles(summary: StatsSummary, clubType: ClubType) {
    val isMovie = clubType == ClubType.MOVIE
    val (reveal, animateIn) = rememberReveal()
    CompositionLocalProvider(LocalAnimateIn provides animateIn) {
        Row(Modifier.fillMaxWidth().then(reveal), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val works = rememberCountUp(summary.workCount)
            SummaryTile(
                icon = if (isMovie) Icons.Outlined.Movie else Icons.AutoMirrored.Outlined.MenuBook,
                value = works.toString(),
                label = if (isMovie) "movies watched" else "books read",
                modifier = Modifier.weight(1f),
            )
            if (isMovie) {
                val minutes = rememberCountUp(summary.runtimeMinutes)
                val totalHours = summary.runtimeMinutes / 60
                val days = if (totalHours > 24) (totalHours / 24.0).let { kotlin.math.round(it).toInt() } else 0
                SummaryTile(
                    icon = Icons.Outlined.Schedule,
                    value = "${minutes / 60}h ${minutes % 60}m",
                    label = "watch time" + if (days > 0) " (${plural(days, "day")})" else "",
                    modifier = Modifier.weight(1f),
                )
            } else if (summary.pageCount > 0) {
                val pages = rememberCountUp(summary.pageCount)
                SummaryTile(Icons.Outlined.Description, formatThousands(pages), "pages read", Modifier.weight(1f))
            }
            summary.clubAverage?.let {
                SummaryTile(Icons.Outlined.StarOutline, fmt(it), "avg score", Modifier.weight(1f), accent = scoreColor())
            }
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
    accent: Color = MaterialTheme.colorScheme.primary,
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// --- Records -----------------------------------------------------------------

@Composable
fun ClubRecordsCard(reviews: List<Review>) {
    val records = remember(reviews) { computeClubRecords(reviews) } ?: return
    StatCard("Club Records") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecordTile("Highest Rated", records.highest, records.highest.value.toFixed1(), goodColor(), Modifier.weight(1f))
            RecordTile("Lowest Rated", records.lowest, records.lowest.value.toFixed1(), badColor(), Modifier.weight(1f))
            records.mostDivisive?.let {
                RecordTile("Most Divisive", it, "±${it.value.toFixed1()} spread", scoreColor(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecordTile(label: String, stat: WorkStat, value: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Poster(stat.work.imageUrl, stat.work.title, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        Text(stat.work.title, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        ValuePill(value, color)
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
    val mode = options.pick(picked)
    val subtitle = when (mode) {
        ScoreMode.DISTRIBUTION -> "How often each score gets handed out, by member"
        ScoreMode.TREND -> "Rolling average score per member over time"
        ScoreMode.AGREEMENT -> "Rolling score spread — lower means more agreement"
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

private enum class ActivityMode { MONTHLY, TOTAL, YEARS }

@Composable
fun ActivityCard(reviews: List<Review>, clubType: ClubType) {
    val monthly = remember(reviews) { computeMonthlyActivity(reviews) }
    val cumulative = remember(reviews) { computeCumulativeCounts(reviews) }
    val yearlyBest = remember(reviews) { computeHighestRatedByYear(reviews) }
    if (monthly.isEmpty()) return
    var mode by rememberSaveable { mutableStateOf(ActivityMode.MONTHLY) }
    val (noun, pluralNoun) = nouns(clubType)
    StatCard(
        "Club Activity",
        subtitle = when (mode) {
            ActivityMode.MONTHLY -> "$pluralNoun reviewed per month"
            ActivityMode.TOTAL -> "Total ${pluralNoun.lowercase()} reviewed over time"
            ActivityMode.YEARS -> "The top-rated $noun from each year your club reviewed"
        },
        controls = {
            SegmentedToggle(
                listOf(ActivityMode.MONTHLY to "Monthly", ActivityMode.TOTAL to "All Time", ActivityMode.YEARS to "Best of Year"),
                mode, { mode = it }, Modifier.fillMaxWidth(),
            )
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
                ActivityMode.YEARS -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    yearlyBest.forEach { entry ->
                        StatRow {
                            Text(
                                "${entry.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(48.dp),
                            )
                            WorkInfo(entry.work, "${plural(entry.workCount, noun)} reviewed", Modifier.weight(1f))
                            ValuePill(entry.average.toFixed1(), goodColor())
                        }
                    }
                }
            }
        }
    }
}

// --- Genres / subjects -------------------------------------------------------

private enum class CategoryMode { RATED, COUNTED }

@Composable
fun GenresCard(reviews: List<Review>, members: List<Member>, clubType: ClubType) {
    if (clubType == ClubType.BOOK) SubjectsCard(reviews) else MovieGenresCard(reviews, members)
}

@Composable
private fun MovieGenresCard(reviews: List<Review>, members: List<Member>) {
    var mode by rememberSaveable { mutableStateOf(CategoryMode.RATED) }
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    val stats = remember(reviews, memberId) { computeGenreStats(reviews, memberId) }
    val counts = remember(reviews) { computeGenreWatchCounts(reviews) }
    val clubStats = remember(reviews) { computeGenreStats(reviews) }
    if (clubStats.mostLoved.isEmpty() && clubStats.leastLoved.isEmpty() && counts.mostWatched.isEmpty()) return
    StatCard("Genres", controls = {
        SegmentedToggle(listOf(CategoryMode.RATED to "Top Rated", CategoryMode.COUNTED to "Most Watched"), mode, { mode = it }, Modifier.fillMaxWidth())
    }) {
        when (mode) {
            CategoryMode.RATED -> {
                if (members.size > 1) MemberFilterChips(members, memberId, { memberId = it })
                if (stats.mostLoved.isNotEmpty()) {
                    SectionLabel("Most Loved", goodColor())
                    RatedCategoryList(stats.mostLoved, "movie", goodColor())
                }
                if (stats.leastLoved.isNotEmpty()) {
                    SectionLabel("Least Loved", badColor())
                    RatedCategoryList(stats.leastLoved, "movie", badColor())
                }
                if (stats.mostLoved.isEmpty()) CardHint("Not enough scores yet: a genre needs two scored movies.")
            }
            CategoryMode.COUNTED -> {
                val max = counts.mostWatched.firstOrNull()?.count ?: 1
                if (counts.mostWatched.isNotEmpty()) {
                    SectionLabel("Most Watched", goodColor())
                    CountedCategoryList(counts.mostWatched, max, goodColor())
                }
                if (counts.leastWatched.isNotEmpty()) {
                    SectionLabel("Least Watched", badColor())
                    CountedCategoryList(counts.leastWatched, max, badColor())
                }
            }
        }
    }
}

@Composable
private fun SubjectsCard(reviews: List<Review>) {
    val scores = remember(reviews) { computeSubjectStats(reviews) }
    val counts = remember(reviews) { computeSubjectCounts(reviews) }
    val options = buildList {
        if (scores.isNotEmpty()) add(CategoryMode.RATED to "Top Rated")
        if (counts.isNotEmpty()) add(CategoryMode.COUNTED to "Most Read")
    }
    if (options.isEmpty()) return
    var picked by rememberSaveable { mutableStateOf(CategoryMode.RATED) }
    val mode = options.pick(picked)
    StatCard(
        "Subjects",
        subtitle = if (mode == CategoryMode.RATED) "Average score per subject (2+ books)" else "The subjects your club reaches for most",
        controls = { SegmentedToggle(options, mode, { picked = it }, Modifier.fillMaxWidth()) },
    ) {
        when (mode) {
            CategoryMode.RATED -> RatedCategoryList(scores, "book", goodColor())
            CategoryMode.COUNTED -> CountedCategoryList(counts, counts.first().count, MaterialTheme.colorScheme.primary)
        }
    }
}

/** Ranked categories with their count and average score (web genre / subject lists). */
@Composable
private fun RatedCategoryList(stats: List<CategoryStat>, noun: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.forEachIndexed { index, stat ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RankNumber(index, color)
                Text(stat.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(plural(stat.count, noun), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ValuePill(fmt(stat.averageScore), color, Modifier.widthIn(min = 48.dp))
            }
        }
    }
}

/** Ranked categories with a bar against the most common one and their count. */
@Composable
private fun CountedCategoryList(counts: List<CategoryCount>, max: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        counts.forEachIndexed { index, count ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RankNumber(index, color)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(count.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    GrowingBar(count.count.toFloat() / max.coerceAtLeast(1), color, index, Modifier.width(96.dp))
                }
                ValuePill("${count.count}", color, Modifier.widthIn(min = 48.dp))
            }
        }
    }
}

@Composable
private fun RankNumber(index: Int, color: Color) {
    Text(
        "${index + 1}",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.width(22.dp),
        textAlign = TextAlign.Center,
    )
}

/** A thin rounded bar that grows in with its row (web `animate-bar-grow`). */
@Composable
private fun GrowingBar(fraction: Float, color: Color, index: Int, modifier: Modifier = Modifier) {
    val growth = rememberBarGrowth(index)
    Box(modifier.height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f) * growth).fillMaxHeight().clip(CircleShape).background(color))
    }
}

// --- People ------------------------------------------------------------------

private enum class PeopleMode { DIRECTORS, ACTORS }

@Composable
fun PeopleCard(reviews: List<Review>, clubType: ClubType, cast: Map<String, List<CreditedPerson>>?) {
    if (clubType == ClubType.BOOK) {
        val authors = remember(reviews) { computeTopAuthors(reviews) }
        if (authors.isEmpty()) return
        StatCard("Most Read Authors", subtitle = "Ranked by appearances in your club's reviews") {
            PersonLeaderboard(authors, itemNoun = "book")
        }
        return
    }
    val directors = remember(reviews) { computeTopDirectors(reviews) }
    val actors = remember(reviews, cast) { cast?.let { computeTopActors(reviews, it) }.orEmpty() }
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
fun ReviewerLeaderboardCard(reviews: List<Review>, members: List<Member>) {
    val entries = remember(reviews, members) { computeLeaderboard(reviews, members) }
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

// --- Taste similarity --------------------------------------------------------

private enum class TasteScope { CLUB, YOU }
private enum class TasteMode { MOST, LEAST }

@Composable
fun TasteSimilarityCard(reviews: List<Review>, members: List<Member>, currentUserId: String?) {
    val club = remember(reviews, members) { computeTasteSimilarity(reviews, members) }
    val yours = remember(reviews, members, currentUserId) {
        currentUserId?.let { computeTasteSimilarity(reviews, members, it) }
    }
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
fun MemberOutliersCard(reviews: List<Review>, members: List<Member>, clubType: ClubType) {
    val guilty = remember(reviews, members) { computeGuiltyPleasures(reviews, members) }
    val curmudgeons = remember(reviews, members) { computeCurmudgeons(reviews, members) }
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
fun ConsensusCard(reviews: List<Review>, members: List<Member>) {
    val consensus = remember(reviews, members) { computeConsensus(reviews, members) }
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
fun TmdbDeviationCard(reviews: List<Review>) {
    val deviation = remember(reviews) { computeTmdbDeviation(reviews) }
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

// --- Shared rows ---------------------------------------------------------------

/** A bordered row inside a widget (web `rounded-lg border bg-background/50`). */
@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Poster, title and a supporting line. */
@Composable
private fun WorkInfo(work: Work, supporting: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Poster(work.imageUrl, work.title, Modifier.width(40.dp), shape = RoundedCornerShape(6.dp))
        Column(Modifier.weight(1f)) {
            Text(work.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
