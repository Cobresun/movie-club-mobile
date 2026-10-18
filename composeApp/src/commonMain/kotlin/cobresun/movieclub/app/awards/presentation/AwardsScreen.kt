package cobresun.movieclub.app.awards.presentation

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.presentation.components.StepIndicator
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.ConfirmSheet
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.userMessage

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
                    message = years.userMessage("load awards"),
                    onRetry = { onAction(AwardsAction.Refresh) },
                )
                is AsyncResult.Success -> if (years.data.isEmpty()) {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        EmptyState(
                            icon = Icons.Outlined.EmojiEvents,
                            title = "No awards yet",
                            message = "Run your own awards season: choose categories like Best Picture or Funniest Movie, nominate the movies you reviewed, vote, and reveal the winners together.",
                            actionLabel = "Start your first awards",
                            onAction = { onAction(AwardsAction.OpenNewAwards) },
                        )
                    }
                } else {
                    when (val season = state.season) {
                        null, is AsyncResult.Loading -> LoadingSkeleton()
                        is AsyncResult.Error -> ErrorState(
                            message = season.userMessage("load the ${state.selectedYear} awards"),
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalTabBarInset.current),
        ) {
            if (season != null) PhaseControls(state, season, onAction)
        }
    }

    state.newAwards?.let { sheet ->
        NewAwardsSheet(
            sheet = sheet,
            hasExistingYears = (state.years as? AsyncResult.Success)?.data?.isNotEmpty() == true,
            onCreate = { year, categories -> onAction(AwardsAction.CreateYear(year, categories)) },
            onRetry = { onAction(AwardsAction.OpenNewAwards) },
            onDismiss = { onAction(AwardsAction.DismissNewAwards) },
        )
    }

    val season = state.loadedSeason
    val category = picking?.let { title -> season?.categories?.firstOrNull { it.title == title } }
    if (season != null && category != null) {
        NominationPickerSheet(
            category = category,
            year = season.year,
            candidates = state.candidatesFor(category),
            hasReviewsForYear = state.reviewed.any { it.reviewedYear == season.year },
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
            if (selected != null) {
                YearPicker(years, selected) { onAction(AwardsAction.SelectYear(it)) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onAction(AwardsAction.OpenNewAwards) },
                    contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New awards")
                }
            }
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
            contentPadding = PaddingValues(top = 4.dp, bottom = (if (season.step.manualNext != null) 168.dp else 32.dp) + LocalTabBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Keeps the add-category field reachable above the keyboard.
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            item(key = "stepper") {
                StepIndicator(season.step, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            item(key = "phase-description") {
                StepIntro(season.step.phase.description, Modifier.animateItem())
            }
            when (season.step) {
                AwardsStep.CATEGORIES -> categoriesStep(season, onAction)
                AwardsStep.NOMINATIONS -> nominationsStep(state, onAction, onPick)
                AwardsStep.RANKINGS -> rankingsStep(state, onAction)
                AwardsStep.PRESENTATION, AwardsStep.COMPLETED -> resultsStep(state, onAction)
            }
            item(key = "delete-year") {
                DeleteYearButton(state, season.year, onAction)
            }
        }
    }
}

/**
 * Sticky footer with the phase's progress, who the club is waiting on, and the
 * button that closes the phase once nobody is outstanding (web `PhaseControls`).
 */
@Composable
private fun PhaseControls(state: AwardsState, season: AwardsSeason, onAction: (AwardsAction) -> Unit) {
    val next = season.step.phase.next ?: return
    var confirming by rememberSaveable { mutableStateOf(false) }
    val waitingOn = state.waitingOn
    val blocked = state.advanceBlocked
    val detail = when {
        waitingOn.isNotEmpty() -> "Waiting on ${listNames(waitingOn.map { it.name })}. The club moves on once everyone is done."
        else -> blocked
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
            Column(Modifier.weight(1f).animateContentSize()) {
                state.phaseProgress?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { confirming = true },
                enabled = blocked == null && !state.isAdvancing,
                contentPadding = PaddingValues(start = 16.dp, end = 10.dp),
            ) {
                if (state.isAdvancing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(next.label)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
    if (confirming) {
        ConfirmSheet(
            title = "${next.label}?",
            message = "${next.confirm} There's no going back.",
            confirmLabel = next.label,
            onConfirm = { onAction(AwardsAction.AdvanceStep) },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun DeleteYearButton(state: AwardsState, year: Int, onAction: (AwardsAction) -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    // Close the sheet once the request settles (on failure the snackbar explains why).
    var wasDeleting by remember { mutableStateOf(false) }
    LaunchedEffect(state.isDeletingYear) {
        if (wasDeleting && !state.isDeletingYear) confirming = false
        wasDeleting = state.isDeletingYear
    }
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
        TextButton(
            onClick = { confirming = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Delete $year awards")
        }
    }
    if (confirming) {
        ConfirmSheet(
            title = "Delete $year awards?",
            message = "This removes every category, nomination and vote for $year. It can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            loading = state.isDeletingYear,
            dismissOnConfirm = false,
            onConfirm = { onAction(AwardsAction.DeleteYear) },
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
