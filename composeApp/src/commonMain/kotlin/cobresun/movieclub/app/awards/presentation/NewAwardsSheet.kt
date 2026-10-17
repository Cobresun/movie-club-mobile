package cobresun.movieclub.app.awards.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import cobresun.movieclub.app.core.presentation.components.rememberAppSheetState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.toUserMessage
import org.koin.compose.koinInject

/** The categories the web suggests, in its order (web `SUGGESTED_CATEGORIES`). */
val SUGGESTED_CATEGORIES = listOf(
    "Best Picture",
    "Worst Picture",
    "Funniest Movie",
    "Best Score",
    "Best Performance",
    "Best Villain",
    "Scariest Movie",
    "Biggest Surprise",
    "Most Rewatchable",
    "Best Cinematography",
)

/** What a new year starts with when the club takes the suggestions. */
val STARTER_CATEGORIES = SUGGESTED_CATEGORIES.take(6)

private enum class Start { SUGGESTED, PREVIOUS, BLANK }

/** Starts a new year of awards (web `NewAwardsModal`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAwardsSheet(
    sheet: NewAwardsState,
    hasExistingYears: Boolean,
    onCreate: (year: Int, categories: List<String>) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    val available = (sheet.availableYears as? AsyncResult.Success)?.data.orEmpty()
    var chosenYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val year = chosenYear?.takeIf { it in available } ?: available.firstOrNull()
    var start by rememberSaveable { mutableStateOf(Start.SUGGESTED) }

    data class Option(val value: Start, val label: String, val detail: String)
    val options = buildList {
        add(Option(Start.SUGGESTED, "Suggested categories", STARTER_CATEGORIES.joinToString(", ")))
        if (sheet.previousCategories.isNotEmpty() && sheet.latestYear != null) {
            add(Option(Start.PREVIOUS, "The same categories as ${sheet.latestYear}", sheet.previousCategories.joinToString(", ")))
        }
        add(Option(Start.BLANK, "No categories", "Add your own from scratch."))
    }
    val selected = start.takeIf { s -> options.any { it.value == s } } ?: Start.SUGGESTED

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberAppSheetState(),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("New awards", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Nominees are the movies the club reviewed during the year you pick. You'll be able to change the categories before nominations open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (val years = sheet.availableYears) {
                AsyncResult.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
                is AsyncResult.Error -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        (years.dataError as? DataError.Remote)?.toUserMessage("load the years") ?: "Couldn't load the years.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
                is AsyncResult.Success -> if (years.data.isEmpty()) {
                    Text(
                        if (hasExistingYears) "Every year the club reviewed movies in already has awards."
                        else "The club hasn't reviewed any movies yet. Once it has, you can hold awards for that year.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel("Year")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            years.data.forEach { y ->
                                FilterChip(
                                    selected = y == year,
                                    onClick = {
                                        if (y != year) haptics.performTick()
                                        chosenYear = y
                                    },
                                    label = { Text("$y", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = if (y == year) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                        Text(
                            "Only years the club reviewed movies in and hasn't held awards for yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (year != null) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Start with")
                    options.forEach { option ->
                        StartOption(
                            label = option.label,
                            detail = option.detail,
                            selected = option.value == selected,
                            onClick = {
                                if (option.value != selected) haptics.performTick()
                                start = option.value
                            },
                        )
                    }
                }
                Button(
                    onClick = {
                        val categories = when (selected) {
                            Start.SUGGESTED -> STARTER_CATEGORIES
                            Start.PREVIOUS -> sheet.previousCategories
                            Start.BLANK -> emptyList()
                        }
                        haptics.performConfirm()
                        onCreate(year, categories)
                    },
                    enabled = !sheet.isCreating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (sheet.isCreating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Start $year awards")
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StartOption(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(if (selected) 2.dp else 1.dp, ring, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Column(Modifier.weight(1f).padding(top = 10.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
