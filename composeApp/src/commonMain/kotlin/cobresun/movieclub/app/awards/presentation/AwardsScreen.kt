package cobresun.movieclub.app.awards.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.isRankedBy
import cobresun.movieclub.app.awards.presentation.components.StepIndicator
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwardsScreen(
    state: AwardsState,
    onAction: (AwardsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf<String?>(null) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(state, onAction)
            when (val years = state.years) {
                is AsyncResult.Loading -> LoadingSkeleton()
                is AsyncResult.Error -> ErrorState(
                    message = (years.dataError as? DataError.Remote)?.toUserMessage("load awards") ?: "Couldn't load awards.",
                    onRetry = { onAction(AwardsAction.Refresh) },
                )
                is AsyncResult.Success -> if (years.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        EmptyState(
                            icon = Icons.Outlined.EmojiEvents,
                            title = "No awards yet",
                            message = "This club hasn't run an awards season. Once a year is opened you'll pick categories, nominate movies and rank them here.",
                            actionLabel = "Check again",
                            onAction = { onAction(AwardsAction.Refresh) },
                        )
                    }
                } else {
                    when (val season = state.season) {
                        null, is AsyncResult.Loading -> LoadingSkeleton()
                        is AsyncResult.Error -> ErrorState(
                            message = (season.dataError as? DataError.Remote)?.toUserMessage("load the ${state.selectedYear} awards")
                                ?: "Couldn't load these awards.",
                            onRetry = { onAction(AwardsAction.Refresh) },
                        )
                        is AsyncResult.Success -> SeasonContent(state, season.data, onAction, onPick = { picking = it.title })
                    }
                }
            }
        }

        val season = state.loadedSeason
        AnimatedVisibility(
            visible = season?.step?.manualNext != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            if (season != null) AdvanceBar(state, season, onAction)
        }
    }

    val season = state.loadedSeason
    val category = picking?.let { title -> season?.categories?.firstOrNull { it.title == title } }
    if (season != null && category != null) {
        NominationPickerSheet(
            category = category,
            year = season.year,
            candidates = state.candidatesFor(category),
            onPick = {
                onAction(AwardsAction.Nominate(category.title, it.movieId))
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun Header(state: AwardsState, onAction: (AwardsAction) -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
        ClubNameLabel(state.clubName)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Awards", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            val years = (state.years as? AsyncResult.Success)?.data.orEmpty()
            val selected = state.selectedYear
            if (selected != null) YearPicker(years, selected) { onAction(AwardsAction.SelectYear(it)) }
        }
    }
}

@Composable
private fun YearPicker(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilledTonalButton(
            onClick = { open = true },
            enabled = years.size > 1,
            contentPadding = PaddingValues(start = 16.dp, end = if (years.size > 1) 8.dp else 16.dp),
        ) {
            Text("$selected", fontWeight = FontWeight.SemiBold)
            if (years.size > 1) Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose year")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text("$year") },
                    trailingIcon = { if (year == selected) Icon(Icons.Default.Check, null) },
                    onClick = { open = false; onSelect(year) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonContent(
    state: AwardsState,
    season: AwardsSeason,
    onAction: (AwardsAction) -> Unit,
    onPick: (AwardCategory) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(AwardsAction.Refresh) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 4.dp, bottom = if (season.step.manualNext != null) 140.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "stepper") {
                StepIndicator(season.step, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            when (season.step) {
                AwardsStep.CATEGORIES -> categoriesStep(season, onAction)
                AwardsStep.NOMINATIONS -> nominationsStep(state, onAction, onPick)
                AwardsStep.RANKINGS -> rankingsStep(state, onAction)
                AwardsStep.PRESENTATION, AwardsStep.COMPLETED -> resultsStep(state, onAction)
            }
        }
    }
}

/** Sticky footer that explains what's left and moves the season to its next phase. */
@Composable
private fun AdvanceBar(state: AwardsState, season: AwardsSeason, onAction: (AwardsAction) -> Unit) {
    val next = season.step.manualNext ?: return
    var confirming by rememberSaveable { mutableStateOf(false) }
    val hint = when (season.step) {
        AwardsStep.CATEGORIES -> if (season.categories.isEmpty()) "Add at least one category to continue."
        else "${season.categories.size} categor${if (season.categories.size == 1) "y" else "ies"} ready."
        AwardsStep.NOMINATIONS -> {
            val nominees = season.categories.sumOf { it.nominations.size }
            "$nominees nominee${if (nominees == 1) "" else "s"} so far."
        }
        AwardsStep.RANKINGS -> {
            val waiting = state.members.count { m -> season.categories.any { !it.isRankedBy(m.id) } }
            if (waiting == 0) "Everyone has ranked every category."
            else "Waiting on $waiting member${if (waiting == 1) "" else "s"} to finish ranking."
        }
        else -> ""
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = { confirming = true }, enabled = state.canAdvance && !state.isAdvancing) {
                if (state.isAdvancing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (next == AwardsStep.PRESENTATION) "Results" else next.title)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = when (next) {
                AwardsStep.NOMINATIONS -> "Open nominations?"
                AwardsStep.RANKINGS -> "Close nominations?"
                else -> "Close rankings?"
            },
            message = when (next) {
                AwardsStep.NOMINATIONS -> "Categories lock in and members can start nominating movies. This moves the season forward for everyone."
                AwardsStep.RANKINGS -> "Nominations close and members start ranking the nominees. This moves the season forward for everyone."
                else -> "Rankings close and the club can reveal the winners. This moves the season forward for everyone."
            },
            confirmLabel = "Continue",
            destructive = false,
            onConfirm = { onAction(AwardsAction.AdvanceStep) },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(44.dp))
        repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(96.dp), MaterialTheme.shapes.large) }
    }
}
