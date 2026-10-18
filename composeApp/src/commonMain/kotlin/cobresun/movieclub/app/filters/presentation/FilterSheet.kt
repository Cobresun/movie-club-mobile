package cobresun.movieclub.app.filters.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import cobresun.movieclub.app.core.presentation.components.rememberAppSheetState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RecentActors
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.filters.domain.ChoiceFilterOption
import cobresun.movieclub.app.filters.domain.FilterIcon
import cobresun.movieclub.app.filters.domain.FilterQuery
import cobresun.movieclub.app.filters.domain.FilterResults
import cobresun.movieclub.app.filters.domain.FilterSelection
import cobresun.movieclub.app.filters.domain.RangeFilterOption
import cobresun.movieclub.app.filters.domain.RangeSection
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Web `FilterPanel` in a bottom sheet: a "Filters" header with Clear all and
 * close, one section per filter (icon, title, range summary, per-section
 * Clear), and a sticky footer button saying what closing will reveal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    query: FilterQuery,
    results: FilterResults<*>,
    onQueryChange: (FilterQuery) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    val sheetState = rememberAppSheetState()
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    val hasSelections = query.selections.keys.any { key -> results.options.any { it.key == key } }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                AnimatedVisibility(hasSelections, enter = fadeIn(tween(AppMotion.FAST)), exit = fadeOut(tween(AppMotion.FAST))) {
                    TextButton(onClick = {
                        haptics.performConfirm()
                        onQueryChange(query.cleared())
                    }) { Text("Clear all", fontWeight = FontWeight.Medium) }
                }
                IconButton(onClick = ::close) {
                    Icon(Icons.Default.Close, contentDescription = "Close filters")
                }
            }

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                results.options.forEach { option ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val selection = query.selections[option.key]
                    Column(Modifier.padding(vertical = 16.dp)) {
                        SectionHeader(
                            icon = option.icon.imageVector,
                            title = option.label,
                            summary = (option as? RangeFilterOption)?.let {
                                RangeSection.summary(it, selection as? FilterSelection.Range)
                            },
                            active = selection != null,
                            onClear = {
                                haptics.performTick()
                                onQueryChange(query.with(option.key, null))
                            },
                        )
                        Spacer(Modifier.size(12.dp))
                        when (option) {
                            is ChoiceFilterOption -> ChoiceFilterSection(
                                option = option,
                                all = results.all,
                                facet = results.facet(option.key),
                                selection = selection as? FilterSelection.Choice,
                                onSelectionChange = { onQueryChange(query.with(option.key, it)) },
                            )

                            is RangeFilterOption -> RangeFilterSection(
                                option = option,
                                all = results.all,
                                facet = results.facet(option.key),
                                selection = selection as? FilterSelection.Range,
                                onSelectionChange = { onQueryChange(query.with(option.key, it)) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = ::close,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .heightIn(min = 48.dp),
            ) {
                AnimatedContent(
                    targetState = results.resultLabel,
                    transitionSpec = { fadeIn(tween(AppMotion.FAST)) togetherWith fadeOut(tween(AppMotion.FAST)) },
                    label = "result-label",
                ) { Text(it) }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    summary: String?,
    active: Boolean,
    onClear: () -> Unit,
) {
    // Tall enough for the Clear button, so the sheet doesn't jump when it appears.
    Row(Modifier.heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        summary?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(active, enter = fadeIn(tween(AppMotion.FAST)), exit = fadeOut(tween(AppMotion.FAST))) {
            TextButton(
                onClick = onClear,
                modifier = Modifier.heightIn(max = 32.dp).semantics { contentDescription = "Clear ${title.lowercase()}" },
            ) {
                Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The web's Material Design Icons, mapped to the closest Material Symbols. */
internal val FilterIcon.imageVector: ImageVector
    get() = when (this) {
        FilterIcon.GENRE -> Icons.Outlined.TheaterComedy
        FilterIcon.SCORE -> Icons.Outlined.StarOutline
        FilterIcon.CALENDAR_RANGE -> Icons.Outlined.DateRange
        FilterIcon.TIMER -> Icons.Outlined.Timer
        FilterIcon.DIRECTOR -> Icons.Outlined.Campaign
        FilterIcon.CAST -> Icons.Outlined.RecentActors
        FilterIcon.STUDIO -> Icons.Outlined.Domain
        FilterIcon.REVIEWED_IN -> Icons.Outlined.EventAvailable
        FilterIcon.AUTHOR -> Icons.Outlined.HistoryEdu
        FilterIcon.SUBJECT -> Icons.Outlined.LocalOffer
        FilterIcon.BOOK_LENGTH -> Icons.AutoMirrored.Outlined.MenuBook
    }
