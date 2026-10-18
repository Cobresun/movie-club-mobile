package cobresun.movieclub.app.club.presentation

import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import cobresun.movieclub.app.core.presentation.components.tabBarOverlaysContent
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import cobresun.movieclub.app.core.presentation.components.AccountButton
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.awards.presentation.AwardsEvent
import cobresun.movieclub.app.awards.presentation.AwardsScreen
import cobresun.movieclub.app.awards.presentation.AwardsViewModel
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.workType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.components.AppSnackbarHost
import cobresun.movieclub.app.core.presentation.components.PlatformTab
import cobresun.movieclub.app.core.presentation.components.PlatformTabBar
import cobresun.movieclub.app.core.presentation.components.LocalSnackbarHostState
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
import cobresun.movieclub.app.search.presentation.ListPick
import cobresun.movieclub.app.search.presentation.ListPickSection
import cobresun.movieclub.app.core.presentation.posterSubtitle
import kotlinx.coroutines.launch
import cobresun.movieclub.app.app.ClubSection
import cobresun.movieclub.app.app.DeepLink
import cobresun.movieclub.app.app.DeepLinks
import cobresun.movieclub.app.core.domain.ShareLinks
import cobresun.movieclub.app.member.presentation.platform.rememberShareSheet
import org.koin.compose.koinInject
import cobresun.movieclub.app.stats.presentation.StatsAction
import cobresun.movieclub.app.stats.presentation.StatsScreen
import cobresun.movieclub.app.stats.presentation.StatsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import cobresun.movieclub.app.core.presentation.OfflineBanner
import cobresun.movieclub.app.core.presentation.OnReconnect
import cobresun.movieclub.app.awards.presentation.AwardsAction
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A club's home: Reviews and Lists tabs with a shared snackbar and add-work
 * sheet. Coordinates the two features (e.g. a list item moved to reviews
 * refreshes the reviews tab) so they stay independent of each other.
 */
@Composable
fun ClubScreenRoot(
    clubSlug: String,
    /** The tab the route names; null reopens the one last used in this club. */
    initialTab: ClubTab?,
    /** A tab to switch to (e.g. coming back from Wrapped); [onTabRequestHandled] clears it. */
    tabRequest: ClubTab?,
    onTabRequestHandled: () -> Unit,
    /** Opens Club Wrapped at a year (0 for the latest). */
    onOpenWrapped: (year: Int) -> Unit = {},
    clubViewModel: ClubViewModel = koinViewModel { parametersOf(clubSlug, initialTab) },
    listsViewModel: ListsViewModel = koinViewModel { parametersOf(clubSlug) },
    reviewsViewModel: ReviewsViewModel = koinViewModel { parametersOf(clubSlug) },
    statsViewModel: StatsViewModel = koinViewModel { parametersOf(clubSlug) },
    awardsViewModel: AwardsViewModel = koinViewModel { parametersOf(clubSlug) },
) {
    val selectedTab by clubViewModel.selectedTab.collectAsStateWithLifecycle()
    LaunchedEffect(tabRequest) {
        val tab = tabRequest ?: return@LaunchedEffect
        clubViewModel.selectTab(tab)
        onTabRequestHandled()
    }
    val tabResolved by clubViewModel.tabResolved.collectAsStateWithLifecycle()
    val reportTab = LocalClubTabShown.current
    LaunchedEffect(selectedTab, tabResolved) { if (tabResolved) reportTab(selectedTab) }
    // Each tab collects its own state below, so one tab's updates don't recompose
    // the shell (and every other tab). The shell only needs Awards' availability.
    val awardsAvailability by remember(awardsViewModel) {
        awardsViewModel.state.map { it.clubName.isNotEmpty() to it.isAvailable }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(false to false)
    val (awardsKnown, awardsAvailable) = awardsAvailability
    val hapticFeedback = koinInject<HapticFeedback>()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shareSheet = rememberShareSheet()
    var addTarget by remember { mutableStateOf<ClubTab?>(null) }
    var addListId by remember { mutableStateOf<String?>(null) }

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
                is ListsEvent.Share -> shareSheet(event.url, event.title)
                is ListsEvent.MovedToReviews -> {
                    reviewsViewModel.refreshQuietly()
                    val onReviews = clubViewModel.selectedTab.value == ClubTab.REVIEWS
                    show(
                        UiMessage(
                            text = "Moved ${event.title} to Reviews",
                            actionLabel = if (onReviews) null else "View",
                            onAction = if (onReviews) null else ({ clubViewModel.selectTab(ClubTab.REVIEWS) }),
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
                is ReviewsEvent.Share -> shareSheet(event.url, event.title)
            }
        }
    }

    // Club / shared-review links for this club (ClubNavHost brought us here).
    val pendingLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        val link = pendingLink as? DeepLink.Club ?: return@LaunchedEffect
        if (link.slug != clubViewModel.clubSlug) return@LaunchedEffect
        DeepLinks.consume(link)
        when (link.section) {
            ClubSection.REVIEWS -> clubViewModel.selectTab(ClubTab.REVIEWS)
            ClubSection.LISTS -> clubViewModel.selectTab(ClubTab.LISTS)
            ClubSection.STATISTICS -> clubViewModel.selectTab(ClubTab.STATS)
            ClubSection.AWARDS -> clubViewModel.selectTab(ClubTab.AWARDS)
            ClubSection.SETTINGS, ClubSection.WRAPPED, null -> Unit
        }
        link.reviewWorkId?.let { reviewsViewModel.onAction(ReviewsAction.OpenDetail(it)) }
    }

    // Awards: movie clubs with the setting on (web clubSections.ts). Re-check on
    // resume since the setting can change in Club settings.
    // The view model checks once on creation; skip the first resume so club open
    // doesn't fetch clubs and features twice.
    var resumedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(awardsViewModel) {
        if (resumedOnce) awardsViewModel.refreshAvailability() else resumedOnce = true
        onPauseOrDispose {}
    }
    LaunchedEffect(awardsViewModel) {
        awardsViewModel.events.collect { event ->
            when (event) {
                is AwardsEvent.Message -> show(event.message)
            }
        }
    }
    // The club name arrives with the availability check, so an empty name means "not known yet":
    // don't bounce a remembered (or linked) Awards tab before the check lands.
    LaunchedEffect(awardsKnown, awardsAvailable, selectedTab) {
        if (awardsKnown && !awardsAvailable && selectedTab == ClubTab.AWARDS) clubViewModel.selectTab(ClubTab.REVIEWS)
    }

    // Back online: answers shown meanwhile came from storage; fetch fresh ones quietly.
    OnReconnect {
        reviewsViewModel.reloadQuietly()
        listsViewModel.reloadQuietly()
        statsViewModel.reloadQuietly()
        awardsViewModel.refreshAvailability()
        awardsViewModel.onAction(AwardsAction.Refresh)
    }

    val stateHolder = rememberSaveableStateHolder()

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        bottomBar = {
            // Wait for the restored tab, so the bar doesn't flick (or pop) from Reviews to it.
            if (!tabResolved) return@Scaffold
            val tabs = remember(awardsAvailable) { buildList {
                add(PlatformTab(ClubTab.REVIEWS.name, "Reviews", Icons.Outlined.StarOutline, Icons.Filled.Star, "star", "star.fill"))
                add(
                    PlatformTab(
                        ClubTab.LISTS.name, "Lists",
                        Icons.AutoMirrored.Outlined.FormatListBulleted, Icons.AutoMirrored.Filled.FormatListBulleted,
                        "list.bullet.rectangle", "list.bullet.rectangle.fill",
                    )
                )
                add(PlatformTab(ClubTab.STATS.name, "Stats", Icons.Outlined.Insights, Icons.Filled.Insights, "chart.bar", "chart.bar.fill"))
                if (awardsAvailable) {
                    add(PlatformTab(ClubTab.AWARDS.name, "Awards", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents, "trophy", "trophy.fill"))
                }
            } }
            PlatformTabBar(
                tabs = tabs,
                selectedKey = selectedTab.name,
                onSelect = { clubViewModel.selectTab(ClubTab.valueOf(it)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            // iOS: let content run under the translucent glass bar; screens pad by LocalTabBarInset.
            modifier = Modifier.fillMaxSize().then(
                if (tabBarOverlaysContent) Modifier.padding(top = padding.calculateTopPadding())
                else Modifier.padding(padding)
            ),
            label = "club-tab",
        ) { tab ->
            // Hold the content back for the moment it takes to restore this club's last tab.
            if (!tabResolved) return@AnimatedContent
            stateHolder.SaveableStateProvider(tab.name) {
              CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalTabBarInset provides if (tabBarOverlaysContent) padding.calculateBottomPadding() else 0.dp,
              ) {
                when (tab) {
                    ClubTab.REVIEWS -> {
                      val reviewsState by reviewsViewModel.state.collectAsStateWithLifecycle()
                      ReviewsScreen(
                        state = reviewsState,
                        onAction = reviewsViewModel::onAction,
                        onAddClick = { addTarget = ClubTab.REVIEWS },
                      )
                    }

                    ClubTab.LISTS -> {
                      val listsState by listsViewModel.state.collectAsStateWithLifecycle()
                      ListsScreen(
                        state = listsState,
                        onAction = listsViewModel::onAction,
                        onAddClick = { listId ->
                            addListId = listId
                            addTarget = ClubTab.LISTS
                        },
                        hapticFeedback = hapticFeedback,
                        onMarkFinished = { work, fromListId ->
                            // Like web: the move shows on Reviews optimistically, so go there now.
                            reviewsViewModel.onAction(ReviewsAction.ShowPending(work))
                            listsViewModel.onAction(ListsAction.MoveToReviews(work, fromListId))
                            clubViewModel.selectTab(ClubTab.REVIEWS)
                        },
                      )
                    }

                    ClubTab.STATS -> {
                        val statsState by statsViewModel.state.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) { statsViewModel.onAction(StatsAction.Shown) }
                        StatsScreen(
                            state = statsState,
                            onAction = statsViewModel::onAction,
                            onGoToReviews = { clubViewModel.selectTab(ClubTab.REVIEWS) },
                            onOpenWrapped = onOpenWrapped,
                            onShare = {
                                shareSheet(ShareLinks.statistics(clubViewModel.clubSlug), "${statsState.clubName} statistics")
                            },
                        )
                    }
                    ClubTab.AWARDS -> {
                        val awardsState by awardsViewModel.state.collectAsStateWithLifecycle()
                        AwardsScreen(
                            state = awardsState,
                            onAction = awardsViewModel::onAction,
                        )
                    }
                }
              }
            }
        }
        // Floats level with each tab's club-name eyebrow (web nav-bar AccountMenu).
        AccountButton(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = padding.calculateTopPadding() + 8.dp, end = 12.dp)
        )
        // Above the tabs' floating Add button, so it never covers it.
        OfflineBanner(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = padding.calculateBottomPadding() + 88.dp)
        )
        }
    }

    addTarget?.let { target ->
        // Collected here, only while the sheet is open (see the tabs above).
        val listsState by listsViewModel.state.collectAsStateWithLifecycle()
        val reviewsState by reviewsViewModel.state.collectAsStateWithLifecycle()
        val toLists = target == ClubTab.LISTS
        val workType = (if (toLists) listsState.clubType else reviewsState.clubType).workType
        val destinationItems = if (toLists) addListId?.let { listsState.itemsOrEmpty(it) } else null
        val existing = remember(toLists, destinationItems, reviewsState.reviews) {
            if (toLists) destinationItems.orEmpty().mapNotNull { it.externalId }.toSet()
            else reviewsState.reviewedExternalIds
        }
        LaunchedEffect(Unit) { listsViewModel.onAction(ListsAction.LoadAllItems) }
        val statusLabels = remember(toLists, listsState.allItems, addListId, reviewsState.reviews) {
            val listed = (listsState.allItems as? cobresun.movieclub.app.core.domain.AsyncResult.Success)?.data.orEmpty()
                .mapNotNull { item -> item.work.externalId?.let { Triple(it, item.listId, item.listTitle) } }
            workStatusLabels(
                reviewedIds = if (toLists) reviewsState.reviewedExternalIds else emptySet(),
                listed = listed,
                destinationListId = if (toLists) addListId else null,
            )
        }
        AddWorkSheet(
            title = if (toLists) "Add to ${listsState.list(addListId)?.title ?: "list"}" else "Add a review",
            workType = workType,
            existingExternalIds = existing,
            statusLabels = statusLabels,
            clubSlug = clubViewModel.clubSlug,
            onPick = { result ->
                val work = result.toNewWork()
                if (toLists) addListId?.let { listsViewModel.onAction(ListsAction.AddWork(work, it)) }
                else reviewsViewModel.onAction(ReviewsAction.AddReview(work))
                addTarget = null
            },
            onDismiss = { addTarget = null },
            fromLists = if (toLists) null else {
                val listed = remember(listsState.allItems, listsState.upNextWorkId) { listsState.pickableItems }
                val picks = remember(listed, listsState.allItems) {
                    listsState.allItems.mapSuccess {
                        listed.map { item ->
                            ListPick(
                                key = item.work.id,
                                title = item.work.title,
                                subtitle = item.work.posterSubtitle(),
                                imageUrl = item.work.imageUrl,
                                listTitle = item.listTitle,
                                isUpNext = item.work.id == listsState.upNextWorkId,
                            )
                        }
                    }
                }
                ListPickSection(
                    items = picks,
                    onPick = { pick ->
                        listed.firstOrNull { it.work.id == pick.key }?.let { item ->
                            reviewsViewModel.onAction(ReviewsAction.ShowPending(item.work))
                            listsViewModel.onAction(ListsAction.MoveToReviews(item.work, item.listId))
                        }
                        addTarget = null
                    },
                )
            },
        )
    }
}

private inline fun <T, R> cobresun.movieclub.app.core.domain.AsyncResult<T>.mapSuccess(
    transform: (T) -> R,
): cobresun.movieclub.app.core.domain.AsyncResult<R> = when (this) {
    is cobresun.movieclub.app.core.domain.AsyncResult.Success -> cobresun.movieclub.app.core.domain.AsyncResult.Success(transform(data))
    is cobresun.movieclub.app.core.domain.AsyncResult.Loading -> cobresun.movieclub.app.core.domain.AsyncResult.Loading
    is cobresun.movieclub.app.core.domain.AsyncResult.Error -> this
}

private fun WorkSearchResult.toNewWork() = NewWork(
    type = type,
    title = title,
    externalId = externalId,
    imageUrl = imageUrl,
)
