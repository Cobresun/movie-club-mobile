package cobresun.movieclub.app.reviews.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.reviews.domain.FactKind
import cobresun.movieclub.app.reviews.domain.ReviewFact

/**
 * Spotlight fact about a review. A gradient hairline frame (primary → secondary)
 * gives it the web card's "special event" treatment without dominating the sheet.
 */
@Composable
fun ReviewFactCard(fact: ReviewFact, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.secondary)), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(fact.kind.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(fact.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.primary)
            Text(fact.text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

private fun FactKind.icon(): ImageVector = when (this) {
    FactKind.ALL_TIME_HIGH -> Icons.Outlined.EmojiEvents
    FactKind.ALL_TIME_LOW -> Icons.Outlined.TrendingDown
    FactKind.CLUB_MILESTONE -> Icons.Outlined.Cake
    FactKind.WATCH_TIME_MILESTONE -> Icons.Outlined.Weekend
    FactKind.PAGES_MILESTONE -> Icons.Outlined.LocalLibrary
    FactKind.DIVISIVE -> Icons.Outlined.Bolt
    FactKind.YEAR_HIGH -> Icons.Outlined.MilitaryTech
    FactKind.YEAR_LOW -> Icons.Outlined.ThumbDown
    FactKind.PERSON_HIGH -> Icons.Outlined.WorkspacePremium
    FactKind.PERSON_LOW -> Icons.Outlined.StarOutline
    FactKind.ACTOR_MILESTONE -> Icons.Outlined.Person
    FactKind.TIME_TRAVEL -> Icons.Outlined.History
    FactKind.LONGEST_RUNTIME -> Icons.Outlined.AccessTime
    FactKind.LONGEST_BOOK -> Icons.Outlined.AutoStories
    FactKind.DECADE_FIRST -> Icons.Outlined.CalendarMonth
    FactKind.FIRST_GENRE -> Icons.Outlined.Explore
    FactKind.TMDB_DEVIATION -> Icons.Outlined.Balance
}
