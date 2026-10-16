package cobresun.movieclub.app.core.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.CastMember
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.metaFacts
import coil3.compose.AsyncImage

/**
 * Cinematic header for a work's detail sheet: backdrop (or a dimmed poster)
 * fading into the sheet, with the poster, title and key facts overlapping it.
 */
@Composable
fun WorkHero(
    work: Work,
    modifier: Modifier = Modifier,
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
                val facts = work.metaFacts()
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  •  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing?.invoke()
            }
        }
    }
}

/** Genres/subjects, credits, tagline and an expandable synopsis. */
@Composable
fun WorkAbout(work: Work, modifier: Modifier = Modifier) {
    val details = work.details ?: return
    val tags: List<String>
    val credit: String?
    val tagline: String?
    val synopsis: String?
    when (details) {
        is WorkDetails.Movie -> {
            tags = details.genres
            credit = details.directors.takeIf { it.isNotEmpty() }?.let { "Directed by ${it.joinToString(", ")}" }
            tagline = details.tagline
            synopsis = details.overview
        }

        is WorkDetails.Book -> {
            tags = details.subjects.take(6)
            credit = details.authors.takeIf { it.isNotEmpty() }?.let { "By ${it.joinToString(", ")}" }
            tagline = null
            synopsis = details.description?.stripHtml()
        }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (credit != null) {
            Text(credit, style = MaterialTheme.typography.titleSmall)
        }
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { TagChip(it) }
            }
        }
        if (!tagline.isNullOrBlank()) {
            Text(
                tagline,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (!synopsis.isNullOrBlank()) {
            var expanded by remember(work.id) { mutableStateOf(false) }
            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { expanded = !expanded },
            )
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

/** Horizontally scrolling cast strip. */
@Composable
fun CastRow(cast: List<CastMember>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        items(cast.take(20), key = { it.name + it.character }) { member ->
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(name = member.name, imageUrl = member.profileImageUrl, size = 64.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    member.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                if (!member.character.isNullOrBlank()) {
                    Text(
                        member.character,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
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

private fun String.stripHtml(): String =
    replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")
        .trim()
