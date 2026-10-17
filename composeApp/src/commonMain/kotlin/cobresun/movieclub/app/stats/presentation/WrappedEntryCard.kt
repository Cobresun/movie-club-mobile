package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.theme.WrappedColors

/**
 * The way into Club Wrapped from the top of the statistics page (web
 * WrappedEntryCard.vue): one chip per released year. Hidden until a year is out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrappedEntryCard(years: List<Int>, onOpen: (year: Int) -> Unit, modifier: Modifier = Modifier) {
    if (years.isEmpty()) return
    val ink = WrappedColors.Ink
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(WrappedColors.Violet))
            .clickable(onClickLabel = "Open ${years.first()} Wrapped") { onOpen(years.first()) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = ink, modifier = Modifier.size(28.dp))
            Column {
                Text("Club Wrapped", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                Text(
                    "Each year's highlights as shareable story cards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink.copy(alpha = 0.8f),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            years.forEach { year ->
                Text(
                    "$year",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ink.copy(alpha = 0.15f))
                        .semantics { contentDescription = "$year Wrapped" }
                        .clickable { onOpen(year) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
