package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.stats.domain.toFixed1
import cobresun.movieclub.app.theme.chartSeriesColor
import kotlin.math.floor

/* Score distribution and activity over time. */

// --- Scores ------------------------------------------------------------------

private enum class ScoreMode { DISTRIBUTION, TREND, AGREEMENT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScoresCard(model: StatsModel, members: List<Member>) {
    val distribution = model.distribution
    val trend = model.trend
    val spread = model.spread
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
                        val lo = series.flatMap { it.second }.minOfOrNull { it.value }?.let { maxOf(0.0, floor(it) - 1) } ?: 0.0
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
internal fun ActivityCard(model: StatsModel, clubType: ClubType) {
    val monthly = model.monthly
    val cumulative = model.cumulative
    val yearlyBest = model.yearlyBest
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
