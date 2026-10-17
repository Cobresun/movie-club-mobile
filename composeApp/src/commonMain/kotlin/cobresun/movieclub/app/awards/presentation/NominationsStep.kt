package cobresun.movieclub.app.awards.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.NOMINATIONS_PER_MEMBER
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.NominationCandidate
import cobresun.movieclub.app.awards.domain.nominationsBy
import cobresun.movieclub.app.awards.presentation.components.Pill
import cobresun.movieclub.app.awards.presentation.components.SectionCard
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.POSTER_ASPECT
import cobresun.movieclub.app.core.presentation.components.Poster

private val SLOT_WIDTH = 104.dp

fun LazyListScope.nominationsStep(
    state: AwardsState,
    onAction: (AwardsAction) -> Unit,
    onPick: (AwardCategory) -> Unit,
) {
    val season = state.loadedSeason ?: return
    val me = state.currentUserId.orEmpty()
    item(key = "nominations-progress") {
        val done = season.categories.count { it.nominationsBy(me).isNotEmpty() }
        NominationProgress(done, season.categories.size)
    }
    if (season.categories.isEmpty()) {
        item(key = "nominations-empty") {
            EmptyState(Icons.Outlined.MovieFilter, "No categories", "This season has no categories to nominate in.")
        }
    }
    items(season.categories, key = { "nominate-${it.title}" }) { category ->
        NominationCard(
            category = category,
            mine = category.nominationsBy(me),
            onAdd = { onPick(category) },
            onRemove = { onAction(AwardsAction.Withdraw(category.title, it.movieId)) },
            modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
        )
    }
}

@Composable
private fun NominationProgress(done: Int, total: Int) {
    SectionCard(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your nominations", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$done / $total categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun NominationCard(
    category: AwardCategory,
    mine: List<Nomination>,
    onAdd: () -> Unit,
    onRemove: (Nomination) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Other members' picks stay private until voting opens, like the web.
            Text(
                category.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Pill("${mine.size}/$NOMINATIONS_PER_MEMBER", emphasized = mine.size == NOMINATIONS_PER_MEMBER)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            mine.forEach { nomination ->
                Box(Modifier.width(SLOT_WIDTH)) {
                    Column {
                        Poster(nomination.posterUrl, nomination.title, Modifier.fillMaxWidth(), highlighted = true)
                        Text(
                            nomination.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                            .clickable(onClickLabel = "Remove ${nomination.title}", role = Role.Button) { onRemove(nomination) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, "Remove ${nomination.title}", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
            }
            repeat(NOMINATIONS_PER_MEMBER - mine.size) {
                EmptySlot(onAdd, "Nominate for ${category.title}")
            }
        }
    }
}

@Composable
private fun EmptySlot(onClick: () -> Unit, label: String) {
    Column(
        Modifier
            .width(SLOT_WIDTH)
            .aspectRatio(POSTER_ASPECT)
            .clip(MaterialTheme.shapes.small)
            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.small)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = label, tint = MaterialTheme.colorScheme.secondary)
        }
        Text(
            "Nominate",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Reviewed movies from the season's year, searchable, tap to nominate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NominationPickerSheet(
    category: AwardCategory,
    year: Int,
    candidates: List<NominationCandidate>,
    /** Whether the club reviewed any movies in [year] at all. */
    hasReviewsForYear: Boolean,
    onPick: (NominationCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = candidates.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("NOMINATE FOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(category.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$year Reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (candidates.size > 6) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search reviewed movies") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (candidates.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.MovieFilter,
                    title = "Nothing to nominate",
                    message = if (hasReviewsForYear) "You've nominated every movie the club reviewed this year."
                    else "The club didn't review any movies in $year, so there's nothing to nominate.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().height(520.dp),
                ) {
                    items(visible, key = { it.movieId }) { candidate ->
                        Column(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClickLabel = "Nominate ${candidate.title}") { onPick(candidate) },
                        ) {
                            Poster(candidate.posterUrl, candidate.title, Modifier.fillMaxWidth())
                            Text(
                                candidate.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            candidate.releaseYear?.let {
                                Text("$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
