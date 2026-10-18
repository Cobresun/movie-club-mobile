package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.stats.domain.toFixed1
import androidx.compose.foundation.layout.RowScope

/*
 * Pieces shared by the statistics widgets (Stats*Widgets.kt). The widgets follow the
 * web's `statisticsWidgets.ts` order and copy; each hides itself when it has nothing to show.
 */

internal fun fmt(v: Double) = ScoreScale.format(v)
internal fun signed1(v: Double) = (if (v > 0) "+" else "") + v.toFixed1()
internal fun plural(n: Int, one: String, many: String = one + "s") = "$n ${if (n == 1) one else many}"

/** Positive / negative accents taken from the theme. */
@Composable internal fun goodColor() = MaterialTheme.colorScheme.secondary
@Composable internal fun badColor() = MaterialTheme.colorScheme.error
@Composable internal fun scoreColor() = MaterialTheme.colorScheme.tertiary

internal fun Member.firstName() = name.trim().substringBefore(' ').ifBlank { name }

internal fun nouns(type: ClubType) = if (type == ClubType.BOOK) "book" to "Books" else "movie" to "Movies"

/** Picks [picked] when it's still offered, else the first option (web's tab fallback). */
internal fun <T> List<Pair<T, String>>.pick(picked: T): T = if (any { it.first == picked }) picked else first().first

// --- Shared rows ---------------------------------------------------------------

/** A bordered row inside a widget (web `rounded-lg border bg-background/50`). */
@Composable
internal fun StatRow(content: @Composable RowScope.() -> Unit) {
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
internal fun WorkInfo(work: Work, supporting: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Poster(work.imageUrl, work.title, Modifier.width(40.dp), shape = RoundedCornerShape(6.dp))
        Column(Modifier.weight(1f)) {
            Text(work.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A thin rounded bar that grows in with its row (web `animate-bar-grow`). */
@Composable
internal fun GrowingBar(fraction: Float, color: Color, index: Int, modifier: Modifier = Modifier) {
    val growth = rememberBarGrowth(index)
    Box(
        modifier.height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .growingFill(color, { fraction.coerceIn(0f, 1f) }, growth)
    )
}
