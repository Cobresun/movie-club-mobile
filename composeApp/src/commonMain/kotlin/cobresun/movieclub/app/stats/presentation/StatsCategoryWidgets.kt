package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.stats.domain.CategoryCount
import cobresun.movieclub.app.stats.domain.CategoryStat
import cobresun.movieclub.app.stats.domain.computeGenreStats

/* Genres (movies) and subjects (books): best-rated and most-watched. */

// --- Genres / subjects -------------------------------------------------------

private enum class CategoryMode { RATED, COUNTED }

@Composable
internal fun GenresCard(model: StatsModel, reviews: List<Review>, members: List<Member>, clubType: ClubType) {
    if (clubType == ClubType.BOOK) SubjectsCard(model) else MovieGenresCard(model, reviews, members)
}

@Composable
private fun MovieGenresCard(model: StatsModel, reviews: List<Review>, members: List<Member>) {
    var mode by rememberSaveable { mutableStateOf(CategoryMode.RATED) }
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    val clubStats = model.genreStats ?: return
    val counts = model.genreWatchCounts ?: return
    // One member's picks are computed on demand; the club's come precomputed.
    val stats = memberId?.let { id -> remember(reviews, id) { computeGenreStats(reviews, id) } } ?: clubStats
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
private fun SubjectsCard(model: StatsModel) {
    val scores = model.subjectStats
    val counts = model.subjectCounts
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
