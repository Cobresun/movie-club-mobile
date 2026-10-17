package cobresun.movieclub.app.reviews.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.CastMember
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.stripHtml
import cobresun.movieclub.app.core.presentation.formatRuntime
import cobresun.movieclub.app.core.presentation.formatShort
import kotlinx.datetime.LocalDate
import kotlin.math.round

/** Web's hero meta line: "1979 · 1h 57m · Horror, Science Fiction" / "1965 · Frank Herbert · 412 pages". */
internal fun Work.reviewMetaLine(): List<String> = when (val d = details) {
    is WorkDetails.Movie -> listOfNotNull(
        year?.toString(),
        d.runtimeMinutes?.let(::formatRuntime),
        d.genres.takeIf { it.isNotEmpty() }?.joinToString(", "),
    )
    is WorkDetails.Book -> listOfNotNull(
        year?.toString(),
        d.authors.takeIf { it.isNotEmpty() }?.joinToString(", "),
        d.pageCount?.let { "$it pages" },
    )
    null -> listOfNotNull(year?.toString())
}

/** The long-form blurb (TMDB overview / book description). */
internal fun Work.overview(): String? = when (val d = details) {
    is WorkDetails.Movie -> d.overview
    is WorkDetails.Book -> d.description?.stripHtml()
    null -> null
}?.takeIf { it.isNotBlank() }

/** Two lines with Read more / Show less (web `WorkDescription`). */
@Composable
internal fun SynopsisSection(overview: String, modifier: Modifier = Modifier) {
    var expanded by remember(overview) { mutableStateOf(false) }
    var overflows by remember(overview) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Synopsis")
        Text(
            overview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.animateContentSize(tween(300)),
        )
        if (overflows || expanded) {
            Text(
                if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(role = Role.Button) { expanded = !expanded },
            )
        }
    }
}

private const val VISIBLE_CAST = 5

/** First five cast members, then "+N More" and "See all (N)" opening the full cast. */
@Composable
internal fun CastSection(cast: List<CastMember>, modifier: Modifier = Modifier) {
    if (cast.isEmpty()) return
    var showAll by remember { mutableStateOf(false) }
    val hasMore = cast.size > VISIBLE_CAST
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Cast", Modifier.weight(1f))
            if (hasMore) {
                Text(
                    "See all (${cast.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(role = Role.Button) { showAll = true },
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            itemsIndexed(cast.take(VISIBLE_CAST), key = { i, m -> "$i-${m.name}" }) { _, member ->
                CastTile(member, avatarSize = 64, Modifier.width(68.dp))
            }
            if (hasMore) {
                item(key = "more") {
                    Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(onClickLabel = "See all cast") { showAll = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+${cast.size - VISIBLE_CAST}", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (showAll) CastSheet(cast) { showAll = false }
}

@Composable
private fun CastTile(member: CastMember, avatarSize: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(name = member.name, imageUrl = member.profileImageUrl, size = avatarSize.dp)
        Spacer(Modifier.height(6.dp))
        Text(member.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (!member.character.isNullOrBlank()) {
            Text(
                member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Full cast in a sheet over the review (web `CastModal`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastSheet(cast: List<CastMember>, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text("Cast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            itemsIndexed(cast, key = { i, m -> "$i-${m.name}" }) { _, member -> CastTile(member, avatarSize = 72) }
        }
    }
}

/**
 * Factual metadata (release date and directors, or first-published year and
 * subjects), the TMDB rating — blurred behind a tap until you've scored, like
 * the club's own scores — and links out to Letterboxd, IMDb and Rotten Tomatoes.
 */
@Composable
internal fun DetailsSection(
    work: Work,
    tmdbHidden: Boolean,
    onRevealTmdb: () -> Unit,
    modifier: Modifier = Modifier,
    /** Letterboxd / IMDb / Rotten Tomatoes chips (the reviews sheet; web lists show none). */
    showLinks: Boolean = true,
    below: @Composable () -> Unit = {},
) {
    val movie = work.details as? WorkDetails.Movie
    val book = work.details as? WorkDetails.Book
    // Movies always qualify (Rotten Tomatoes is a title search); books only with facts to show.
    if (movie == null && (book == null || (book.firstPublishYear == null && book.subjects.isEmpty()))) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Details")
        val facts = buildList<Pair<String, @Composable () -> Unit>> {
            movie?.releaseDate?.let { raw ->
                val date = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
                add("Released" to { FactValue(date?.formatShort() ?: raw) })
            }
            movie?.directors?.takeIf { it.isNotEmpty() }?.let { d ->
                add((if (d.size > 1) "Directors" else "Director") to { FactValue(d.joinToString(", ")) })
            }
            book?.firstPublishYear?.let { add("First published" to { FactValue(it.toString()) }) }
            movie?.voteAverage?.takeIf { it > 0 }?.let { vote ->
                add("TMDB rating" to { TmdbRating(round(vote * 10) / 10, tmdbHidden, onRevealTmdb) })
            }
        }
        facts.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FactLabel(label)
                        value()
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        book?.subjects?.takeIf { it.isNotEmpty() }?.let { SubjectsFact(it) }
        if (movie != null && showLinks) {
            val uriHandler = LocalUriHandler.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                work.externalId?.takeIf { it.isNotBlank() }?.let { id ->
                    ExternalLinkChip("Letterboxd") { uriHandler.openUri("https://letterboxd.com/tmdb/$id/") }
                }
                movie.imdbId?.let { id ->
                    ExternalLinkChip("IMDb") { uriHandler.openUri("https://www.imdb.com/title/$id/") }
                }
                if (work.title.isNotBlank()) {
                    ExternalLinkChip("Rotten Tomatoes") {
                        uriHandler.openUri("https://www.rottentomatoes.com/search?search=${work.title.encodeUriComponent()}")
                    }
                }
            }
        }
        below()
    }
}

@Composable
private fun FactLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FactValue(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun TmdbRating(value: Double, hidden: Boolean, onReveal: () -> Unit) {
    Row(
        Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(enabled = hidden, onClickLabel = "Reveal TMDB rating", onClick = onReveal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hidden) {
            HiddenScoreIcon(Modifier.size(18.dp))
            Text("Tap to reveal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                buildAnnotatedString {
                    append(value.toString())
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append("/10") }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val VISIBLE_SUBJECTS = 6

@Composable
private fun SubjectsFact(subjects: List<String>) {
    var showAll by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.animateContentSize()) {
        FactLabel("Subjects")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            (if (showAll) subjects else subjects.take(VISIBLE_SUBJECTS)).forEach { subject ->
                Text(
                    subject,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            if (subjects.size > VISIBLE_SUBJECTS) {
                TextButton(onClick = { showAll = !showAll }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(if (showAll) "Show less" else "+${subjects.size - VISIBLE_SUBJECTS} more", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ExternalLinkChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Opens in browser",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/** RFC 3986 percent-encoding of everything but unreserved characters (JS `encodeURIComponent`-like). */
internal fun String.encodeUriComponent(): String = buildString {
    for (byte in this@encodeUriComponent.encodeToByteArray()) {
        val c = byte.toInt().toChar()
        if (byte >= 0 && (c.isLetterOrDigit() || c in "-_.!~*'()")) append(c)
        else append('%').append(((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1).uppercase())
    }
}
