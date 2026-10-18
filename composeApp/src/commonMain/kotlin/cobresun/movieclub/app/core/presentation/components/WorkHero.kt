package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.metaFacts
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon

/**
 * Cinematic header for a work's detail sheet: backdrop (or a dimmed poster)
 * fading into the sheet, with the poster, title and key facts overlapping it.
 */
@Composable
fun WorkHero(
    work: Work,
    modifier: Modifier = Modifier,
    /** When set, the TMDB rating shows as a tap-to-reveal placeholder (reviews only, like web). */
    onRevealTmdbRating: (() -> Unit)? = null,
    /** Replaces the default year/runtime/rating facts line (the reviews sheet shows web's meta line). */
    facts: List<String>? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val backdrop = (work.details as? WorkDetails.Movie)?.backdropUrl ?: work.imageUrl
    val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8f)
        ) {
            if (backdrop != null) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.7f),
                )
            }
            Box(Modifier.fillMaxSize().background(bottomScrim(sheetColor, startFraction = 0.05f)))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.small, shadowElevation = 12.dp) {
                Poster(imageUrl = work.imageUrl, title = work.title, modifier = Modifier.width(108.dp))
            }
            Column(
                modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val facts = facts ?: work.metaFacts(includeTmdbRating = onRevealTmdbRating == null)
                val hasTmdbRating = ((work.details as? WorkDetails.Movie)?.voteAverage ?: 0.0) > 0
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  •  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onRevealTmdbRating != null && hasTmdbRating) {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClickLabel = "Reveal TMDB rating", onClick = onRevealTmdbRating)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("TMDB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = "Hidden TMDB rating, tap to reveal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Uppercase small section label used throughout detail sheets. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.offset(y = 0.dp),
    )
}

internal fun String.stripHtml(): String =
    replace(BR_TAG, "\n")
        .replace(P_CLOSE, "\n\n")
        .replace(ANY_TAG, "")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")
        .trim()

// Compiled once, not on every recomposition of the sheets that show synopses.
private val BR_TAG = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val P_CLOSE = Regex("</p>", RegexOption.IGNORE_CASE)
private val ANY_TAG = Regex("<[^>]+>")
