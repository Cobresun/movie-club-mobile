package cobresun.movieclub.app.filters.presentation

import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.filters.domain.ChoiceFilterOption
import cobresun.movieclub.app.filters.domain.ChoiceSection
import cobresun.movieclub.app.filters.domain.FilterSelection
import cobresun.movieclub.app.filters.domain.FilterTarget
import cobresun.movieclub.app.filters.domain.RangeFilterOption
import cobresun.movieclub.app.filters.domain.RangeScale
import cobresun.movieclub.app.filters.domain.RangeSection
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val BAR_MIN_HEIGHT = 3f
private const val BAR_MAX_HEIGHT = 56f

/**
 * Web `ChoiceFilterSection`: chips with counts (zero-match values disabled
 * unless picked), a search field once the list runs past 20 values, and a
 * fold to the top 12 with "Show all N".
 */
@Composable
fun ChoiceFilterSection(
    option: ChoiceFilterOption,
    /** Every work: which values exist, and their order. */
    all: List<FilterTarget>,
    /** Works left by the other filters: the count beside each value. */
    facet: List<FilterTarget>,
    selection: FilterSelection.Choice?,
    onSelectionChange: (FilterSelection.Choice?) -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    var search by rememberSaveable(option.key) { mutableStateOf("") }
    var expanded by rememberSaveable(option.key) { mutableStateOf(false) }
    val ordered = remember(option, all) { ChoiceSection.orderedValues(option, all) }
    val counts = remember(option, facet) { ChoiceSection.tally(option, facet) }
    val selected = selection?.values.orEmpty()
    val isLong = ChoiceSection.isLong(ordered)
    val visible = ChoiceSection.visibleValues(ordered, selected, search, expanded)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isLong) {
            TextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(option.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = option.searchPlaceholder },
            )
        }

        if (visible.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth().animateContentSize(tween(AppMotion.BASE, easing = AppMotion.Emphasized)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                // Chips already carry a 48dp touch target, which spaces the rows.
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                visible.forEach { value ->
                    val isSelected = value in selected
                    val count = counts[value] ?: 0
                    ValueChip(
                        label = value,
                        trailing = count.toString(),
                        selected = isSelected,
                        enabled = isSelected || count > 0,
                        onClick = {
                            haptics.performTick()
                            onSelectionChange(ChoiceSection.toggle(selection, value))
                        },
                    )
                }
            }
        } else {
            Text(
                "Nothing matches “${search.trim()}”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isLong && search.isBlank()) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(max = 36.dp)) {
                Text(
                    if (expanded) "Show fewer" else "Show all ${ordered.size}",
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Web `RangeFilterSection`: preset chips, a histogram of the values the other
 * filters leave (up to 30 bars), and a two-thumb slider snapping to the
 * option's step, with a haptic detent per step while dragging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeFilterSection(
    option: RangeFilterOption,
    /** Every work: fixes the slider's ends and the presets. */
    all: List<FilterTarget>,
    /** Works left by the other filters: what the histogram draws. */
    facet: List<FilterTarget>,
    selection: FilterSelection.Range?,
    onSelectionChange: (FilterSelection.Range?) -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    val values = remember(option, all) { RangeSection.values(option, all) }
    val scale = remember(option, values) { RangeScale.of(option, values) } ?: return
    val presets = remember(option, values) { option.presets(values) }
    val bars = scale.bars(option, facet, selection)
    val busiest = (bars.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val from = scale.fromValue(selection)
    val to = scale.toValue(selection)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (presets.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                presets.forEach { preset ->
                    val active = RangeSection.isPresetActive(preset, selection)
                    ValueChip(
                        label = preset.label,
                        trailing = preset.detail,
                        selected = active,
                        enabled = active || RangeSection.presetHasMatches(option, preset, facet),
                        showCheck = false,
                        onClick = {
                            haptics.performTick()
                            onSelectionChange(RangeSection.togglePreset(preset, selection))
                        },
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(BAR_MAX_HEIGHT.dp).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                val target = if (bar.count == 0) BAR_MIN_HEIGHT
                else BAR_MIN_HEIGHT + bar.count.toFloat() / busiest * (BAR_MAX_HEIGHT - BAR_MIN_HEIGHT)
                val height by animateDpAsState(target.dp, spring(stiffness = 600f))
                val color by animateColorAsState(
                    when {
                        bar.selected -> MaterialTheme.colorScheme.primary
                        selection != null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    },
                    tween(AppMotion.FAST),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(height)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(color),
                )
            }
        }

        // The fill only shows once a span is picked, like web's bare rail.
        val colors = SliderDefaults.colors(
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
            activeTrackColor = if (selection != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        // The slider works in rail fractions so a fractional step (scores) snaps exactly.
        val stepCount = ((scale.hi - scale.lo) / option.step).roundToInt()
        RangeSlider(
            value = scale.fraction(from).toFloat()..scale.fraction(to).toFloat(),
            onValueChange = { range ->
                val nextFrom = scale.valueAt(range.start.toDouble(), option.step)
                val nextTo = maxOf(scale.valueAt(range.endInclusive.toDouble(), option.step), nextFrom)
                if (nextFrom != from || nextTo != to) {
                    haptics.performTick()
                    onSelectionChange(scale.selectionFor(nextFrom, nextTo))
                }
            },
            steps = (stepCount - 1).coerceAtLeast(0),
            startThumb = { RangeThumb() },
            endThumb = { RangeThumb() },
            track = { sliderState ->
                SliderDefaults.Track(
                    rangeSliderState = sliderState,
                    colors = colors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 3.dp,
                    modifier = Modifier.height(6.dp),
                )
            },
            colors = colors,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = option.label
                stateDescription = "${option.format(from)} to ${option.format(to)}"
            },
        )

        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(option.format(scale.lo), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(option.format(scale.hi), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Web's round range thumb (a light disc ringed in brand blue), centred in a 32dp box so
 * it's easy to catch even resting on either end of the rail.
 */
@Composable
private fun RangeThumb() {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(22.dp)
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

/** A pill chip: filled brand blue with a check when picked, dimmed when it would lead nowhere. */
@Composable
private fun ValueChip(
    label: String,
    trailing: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    showCheck: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                trailing?.let {
                    Text(
                        " $it",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(0.65f).padding(start = 2.dp),
                    )
                }
            }
        },
        leadingIcon = if (selected && showCheck) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
        } else {
            null
        },
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    )
}
