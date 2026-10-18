package cobresun.movieclub.app.stats.presentation

import cobresun.movieclub.app.core.domain.groupThousands
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.stats.domain.StatsSummary
import cobresun.movieclub.app.stats.domain.WorkStat
import cobresun.movieclub.app.stats.domain.toFixed1
import kotlin.math.round

/* Headline numbers and club records at the top of the statistics page. */

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
                val days = if (totalHours > 24) (totalHours / 24.0).let { round(it).toInt() } else 0
                SummaryTile(
                    icon = Icons.Outlined.Schedule,
                    value = "${minutes / 60}h ${minutes % 60}m",
                    label = "watch time" + if (days > 0) " (${plural(days, "day")})" else "",
                    modifier = Modifier.weight(1f),
                )
            } else if (summary.pageCount > 0) {
                val pages = rememberCountUp(summary.pageCount)
                SummaryTile(Icons.Outlined.Description, groupThousands(pages.toLong()), "pages read", Modifier.weight(1f))
            }
            summary.clubAverage?.let {
                SummaryTile(Icons.Outlined.StarOutline, fmt(it), "avg score", Modifier.weight(1f), accent = scoreColor())
            }
        }
    }
}


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
internal fun ClubRecordsCard(model: StatsModel) {
    val records = model.records ?: return
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
