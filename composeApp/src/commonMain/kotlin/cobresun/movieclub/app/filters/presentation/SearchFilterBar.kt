package cobresun.movieclub.app.filters.presentation

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import cobresun.movieclub.app.filters.domain.FilterQuery
import cobresun.movieclub.app.filters.domain.FilterResults
import org.koin.compose.koinInject

/**
 * Web `SearchFilterBar`: a title search beside a "Filters" button (tune icon,
 * with a count badge), the applied filters as removable chips beneath, and
 * the filter panel as a bottom sheet (the web's mobile `VBottomSheet`).
 */
@Composable
fun SearchFilterBar(
    query: FilterQuery,
    results: FilterResults<*>,
    onQueryChange: (FilterQuery) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    /** Side gutter; 0 when the parent already pads (a grid's content padding). */
    horizontalPadding: Dp = 16.dp,
    /** Extra buttons after the Filters button (web's `action-button` slot). */
    actions: @Composable () -> Unit = {},
) {
    val haptics = koinInject<HapticFeedback>()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val chips = results.chips
    // The field owns its text while focused: echoing keystrokes back through the
    // view model lands a frame late, which resets the cursor on iOS and makes typing
    // lag. Outside changes (a cleared filter) are picked up while it isn't focused.
    var text by rememberSaveable { mutableStateOf(query.text) }
    var focused by remember { mutableStateOf(false) }
    if (!focused && text != query.text) text = query.text
    val setText = { value: String ->
        text = value
        onQueryChange(query.copy(text = value))
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = text,
                onValueChange = setText,
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { setText("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            )
            if (results.options.isNotEmpty()) {
                FiltersButton(activeCount = chips.size, onClick = { sheetOpen = true })
            }
            actions()
        }

        AnimatedVisibility(
            visible = chips.isNotEmpty(),
            enter = fadeIn(tween(AppMotion.BASE)) + expandVertically(tween(AppMotion.BASE, easing = AppMotion.Emphasized)),
            exit = fadeOut(tween(AppMotion.FAST)) + shrinkVertically(tween(AppMotion.BASE, easing = AppMotion.Emphasized)),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(chips, key = { it.id }) { chip ->
                    InputChip(
                        selected = true,
                        onClick = {
                            haptics.performTick()
                            onQueryChange(query.without(chip))
                        },
                        label = { Text(chip.text) },
                        leadingIcon = {
                            Icon(chip.icon.imageVector, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            enabled = true,
                            selected = true,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderWidth = 1.dp,
                        ),
                        modifier = Modifier
                            .animateItem(fadeInSpec = tween(AppMotion.BASE), placementSpec = tween(AppMotion.BASE, easing = AppMotion.Emphasized), fadeOutSpec = tween(AppMotion.FAST))
                            .semantics { contentDescription = "Remove ${chip.label}: ${chip.text}" },
                    )
                }
                item(key = "clear-all") {
                    TextButton(
                        onClick = {
                            haptics.performConfirm()
                            onQueryChange(query.cleared())
                        },
                        modifier = Modifier.animateItem(),
                    ) {
                        Text("Clear all", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        FilterSheet(
            query = query,
            results = results,
            onQueryChange = onQueryChange,
            onDismiss = { sheetOpen = false },
        )
    }
}

/** The tune button: outlined, filled with the brand tint while filters apply, with a badge that pops on change. */
@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val active = activeCount > 0
    val border by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        tween(AppMotion.FAST),
    )
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        tween(AppMotion.FAST),
    )
    Box {
        Box(
            Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(container)
                .border(BorderStroke(2.dp, border), MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "Open filters", onClick = onClick)
                .semantics { contentDescription = if (active) "Filters, $activeCount active" else "Filters" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Tune, contentDescription = null)
        }
        AnimatedVisibility(
            visible = active,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut(tween(AppMotion.FAST)) + fadeOut(tween(AppMotion.FAST)),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
        ) {
            CountBadge(activeCount)
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    val reduced = rememberReducedMotion()
    val pop = remember { Animatable(1f) }
    var last by remember { mutableStateOf(count) }
    LaunchedEffect(count) {
        if (count != last && !reduced) {
            pop.snapTo(1.3f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        last = count
    }
    Box(
        Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
