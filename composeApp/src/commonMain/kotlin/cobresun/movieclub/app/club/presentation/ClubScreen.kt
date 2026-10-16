package cobresun.movieclub.app.club.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.workType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.lists.presentation.ListsAction
import cobresun.movieclub.app.lists.presentation.ListsEvent
import cobresun.movieclub.app.lists.presentation.ListsScreen
import cobresun.movieclub.app.lists.presentation.ListsViewModel
import cobresun.movieclub.app.reviews.presentation.ReviewsAction
import cobresun.movieclub.app.reviews.presentation.ReviewsEvent
import cobresun.movieclub.app.reviews.presentation.ReviewsScreen
import cobresun.movieclub.app.reviews.presentation.ReviewsViewModel
import cobresun.movieclub.app.search.domain.WorkSearchResult
import cobresun.movieclub.app.search.presentation.AddWorkSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * A club's home: Reviews and Lists tabs with a shared snackbar and add-work
 * sheet. Coordinates the two features (e.g. a list item moved to reviews
 * refreshes the reviews tab) so they stay independent of each other.
 */
@Composable
fun ClubScreenRoot(
    clubViewModel: ClubViewModel = koinViewModel(),
    listsViewModel: ListsViewModel = koinViewModel(),
    reviewsViewModel: ReviewsViewModel = koinViewModel(),
) {
    val selectedTab by clubViewModel.selectedTab.collectAsStateWithLifecycle()
    val listsState by listsViewModel.state.collectAsStateWithLifecycle()
    val reviewsState by reviewsViewModel.state.collectAsStateWithLifecycle()
    val hapticFeedback = koinInject<HapticFeedback>()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var addTarget by remember { mutableStateOf<ClubTab?>(null) }

    fun show(message: UiMessage) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                withDismissAction = message.actionLabel == null,
                duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }

    LaunchedEffect(listsViewModel) {
        listsViewModel.events.collect { event ->
            when (event) {
                is ListsEvent.Message -> show(event.message)
                is ListsEvent.MovedToReviews -> {
                    reviewsViewModel.refreshQuietly()
                    show(
                        UiMessage(
                            text = "Moved ${event.title} to Reviews",
                            actionLabel = "View",
                            onAction = { clubViewModel.selectTab(ClubTab.REVIEWS) },
                        )
                    )
                }
            }
        }
    }
    LaunchedEffect(reviewsViewModel) {
        reviewsViewModel.events.collect { event ->
            when (event) {
                is ReviewsEvent.Message -> show(event.message)
            }
        }
    }

    val stateHolder = rememberSaveableStateHolder()

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = MaterialTheme.shapes.small,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                )
                NavigationBarItem(
                    selected = selectedTab == ClubTab.REVIEWS,
                    onClick = { clubViewModel.selectTab(ClubTab.REVIEWS) },
                    icon = {
                        Icon(
                            if (selectedTab == ClubTab.REVIEWS) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Reviews") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = selectedTab == ClubTab.LISTS,
                    onClick = { clubViewModel.selectTab(ClubTab.LISTS) },
                    icon = {
                        Icon(
                            if (selectedTab == ClubTab.LISTS) Icons.AutoMirrored.Filled.FormatListBulleted
                            else Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Lists") },
                    colors = colors,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            modifier = Modifier.fillMaxSize().padding(padding),
            label = "club-tab",
        ) { tab ->
            stateHolder.SaveableStateProvider(tab.name) {
                when (tab) {
                    ClubTab.REVIEWS -> ReviewsScreen(
                        state = reviewsState,
                        onAction = reviewsViewModel::onAction,
                        onAddClick = { addTarget = ClubTab.REVIEWS },
                    )

                    ClubTab.LISTS -> ListsScreen(
                        state = listsState,
                        onAction = listsViewModel::onAction,
                        onAddClick = { addTarget = ClubTab.LISTS },
                        hapticFeedback = hapticFeedback,
                    )
                }
            }
        }
    }

    addTarget?.let { target ->
        val toLists = target == ClubTab.LISTS
        val workType = (if (toLists) listsState.clubType else reviewsState.clubType).workType
        val existing = if (toLists) {
            (listsState.selectedItems as? cobresun.movieclub.app.core.domain.AsyncResult.Success)
                ?.data.orEmpty().mapNotNull { it.externalId }.toSet()
        } else {
            reviewsState.reviewedExternalIds
        }
        AddWorkSheet(
            title = if (toLists) "Add to ${listsState.selectedList?.title ?: "list"}" else "Add a review",
            workType = workType,
            existingExternalIds = existing,
            onPick = { result ->
                val work = result.toNewWork()
                if (toLists) listsViewModel.onAction(ListsAction.AddWork(work))
                else reviewsViewModel.onAction(ReviewsAction.AddReview(work))
                addTarget = null
            },
            onDismiss = { addTarget = null },
        )
    }
}

private fun WorkSearchResult.toNewWork() = NewWork(
    type = type,
    title = title,
    externalId = externalId,
    imageUrl = imageUrl,
)
