package cobresun.movieclub.app.reviews.presentation

import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.VisibilityOff
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.workType
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.GridSkeleton
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.filters.presentation.SearchFilterBar
import cobresun.movieclub.app.core.presentation.components.tabBarOverlaysContent
import cobresun.movieclub.app.core.presentation.AppMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    state: ReviewsState,
    onAction: (ReviewsAction) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workType = state.clubType.workType
    LaunchedEffect(Unit) { onAction(ReviewsAction.Shown) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
                ClubNameLabel(state.clubName)
                Text("Reviews", style = MaterialTheme.typography.headlineMedium)
            }
            when (val reviews = state.reviews) {
                is AsyncResult.Loading -> GridSkeleton(minCellWidth = 150.dp)
                is AsyncResult.Error -> ErrorState(
                    message = (reviews.dataError as? DataError.Remote)?.toUserMessage("load reviews")
                        ?: "Couldn't load reviews.",
                    onRetry = { onAction(ReviewsAction.Refresh) },
                )

                is AsyncResult.Success -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onAction(ReviewsAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val visible = state.visibleReviews
                        val gridState = rememberLazyGridState()
                        // Keyed items keep the first visible card anchored when the order
                        // changes, which lands the user mid-list. Jump back to the top instead.
                        var lastOrdering by remember { mutableStateOf(state.sortState to state.filterQuery) }
                        LaunchedEffect(state.sortState, state.filterQuery) {
                            val ordering = state.sortState to state.filterQuery
                            if (ordering != lastOrdering) {
                                lastOrdering = ordering
                                gridState.scrollToItem(0)
                            }
                        }
                        // Cards that just joined the visible set (a new review, a cleared search)
                        // fade in from a slight shrink; ones scrolled into view don't.
                        val visibleKeys = visible.map { it.work.id }.toSet()
                        var previousKeys by remember { mutableStateOf<Set<String>?>(null) }
                        val entering = remember(visibleKeys) { previousKeys?.let { visibleKeys - it }.orEmpty() }
                        SideEffect { previousKeys = visibleKeys }
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp + LocalTabBarInset.current),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Scrolls away with the cards rather than pinning above them.
                            item(key = "search-sort", span = { GridItemSpan(maxLineSpan) }) {
                                SearchAndSort(state = state, onAction = onAction)
                            }
                            state.upNext?.let { upNext ->
                                item(key = "up-next", span = { GridItemSpan(maxLineSpan) }) {
                                    UpNextReviewCard(
                                        item = upNext,
                                        starting = state.startingUpNext != null,
                                        onReview = { onAction(ReviewsAction.StartUpNextReview) },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                            items(state.pendingWorks, key = { it.id }, contentType = { "pending" }) { work ->
                                PendingReviewCard(
                                    work,
                                    Modifier
                                        .animateItem(fadeInSpec = tween(200), placementSpec = tween(300, easing = AppMotion.Emphasized))
                                        .scaleInOnEnter(true),
                                )
                            }
                            if (reviews.data.isEmpty() && state.pendingWorks.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyState(
                                        icon = Icons.Outlined.RateReview,
                                        title = "No reviews yet",
                                        message = "Start building your club's ${workType.noun} collection by adding your first review",
                                        actionLabel = "Add review",
                                        onAction = onAddClick,
                                    )
                                }
                            } else if (visible.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyState(
                                        icon = Icons.Outlined.SearchOff,
                                        title = "No results found",
                                        message = "Try adjusting your search or filters. You can search by " +
                                            if (workType == WorkType.BOOK) "title, author, subject, or published year"
                                            else "title, genre, company, director, actor, or release year",
                                    )
                                }
                            }
                            items(visible, key = { it.work.id }, contentType = { "review" }) { review ->
                                ReviewCard(
                                    review = review,
                                    members = state.members,
                                    currentUserId = state.currentUserId,
                                    revealed = state.isRevealed(review),
                                    onClick = { onAction(ReviewsAction.OpenDetail(review.work.id)) },
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = tween(200),
                                            placementSpec = tween(300, easing = AppMotion.Emphasized),
                                            fadeOutSpec = null,
                                        )
                                        .scaleInOnEnter(review.work.id in entering),
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.reviews is AsyncResult.Success,
            enter = fadeIn(),
            exit = fadeOut(),
            // iOS: the glass tab bar's frame already includes its own margin, so the
            // button sits just above it like a native floating action, not 20pt up.
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = LocalTabBarInset.current)
                .padding(if (tabBarOverlaysContent) PaddingValues(end = 16.dp, bottom = 8.dp) else PaddingValues(20.dp)),
        ) {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add review") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }

    state.detail?.let { detail ->
        state.review(detail.workId)?.let { review ->
            ReviewDetailSheet(
                review = review,
                detail = detail,
                members = state.members,
                currentUserId = state.currentUserId,
                discussionQuestionsEnabled = state.discussionQuestionsEnabled,
                scoredWorks = state.myScoredWorks,
                clubSlug = state.clubSlug,
                revealed = state.isRevealed(review),
                canReveal = state.canReveal(review),
                fact = remember(review, state.reviews, state.currentUserId, state.revealedWorkIds) { state.spotlightFact(review) },
                onAction = onAction,
                onDismiss = { onAction(ReviewsAction.CloseDetail) },
            )
        }
    }
}

@Composable
private fun SearchAndSort(state: ReviewsState, onAction: (ReviewsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchFilterBar(
            query = state.filterQuery,
            results = state.filters,
            onQueryChange = { onAction(ReviewsAction.Filter(it)) },
            placeholder = "Search reviews",
            horizontalPadding = 0.dp,
        )
        SortBar(
            sortState = state.sortState,
            members = state.members,
            onSort = { onAction(ReviewsAction.Sort(it)) },
            onToggleDirection = { onAction(ReviewsAction.ToggleSortDirection) },
        )
    }
}

/**
 * Web gallery sort bar: a "Sort by" / "Sorted by X" menu button, then — while a
 * sort is active — a direction pill worded for what's sorted ("Newest first",
 * "Highest first") that reverses it, and Clear.
 */
@Composable
private fun SortBar(
    sortState: ReviewSortState,
    members: List<Member>,
    onSort: (ReviewSortOption) -> Unit,
    onToggleDirection: () -> Unit,
) {
    data class Opt(val option: ReviewSortOption, val label: String, val member: Member? = null)
    val options = listOf(Opt(ReviewSortOption.DateReviewed, "Date reviewed")) +
        members.map { Opt(ReviewSortOption.MemberScore(it.id), "${it.name}'s rating", it) } +
        Opt(ReviewSortOption.AverageScore, "Average rating")
    val active = options.firstOrNull { it.option == sortState.option }
    var menuOpen by remember { mutableStateOf(false) }
    val pillBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            OutlinedButton(
                onClick = { menuOpen = true },
                border = pillBorder,
                contentPadding = PaddingValues(start = 14.dp, end = 8.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text(if (active != null) "Sorted by " else "Sort by", color = MaterialTheme.colorScheme.onSurface)
                active?.let { Text(it.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    "Sort reviews by",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label) },
                        leadingIcon = {
                            when {
                                opt.member != null -> Avatar(opt.member.name, opt.member.imageUrl, size = 24.dp)
                                opt.option == ReviewSortOption.AverageScore ->
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                else -> Icon(Icons.Outlined.Schedule, contentDescription = null)
                            }
                        },
                        trailingIcon = if (opt.option == sortState.option) {
                            { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        onClick = {
                            menuOpen = false
                            onSort(opt.option)
                        },
                    )
                }
            }
        }
        AnimatedVisibility(visible = active != null, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onToggleDirection,
                    border = pillBorder,
                    contentPadding = PaddingValues(start = 8.dp, end = 14.dp),
                    modifier = Modifier.height(36.dp).semantics {
                        stateDescription = "Currently ${sortState.directionLabel.lowercase()}"
                        onClick(label = "Reverse") { onToggleDirection(); true }
                    },
                ) {
                    Icon(
                        if (sortState.descending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(sortState.directionLabel, color = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedButton(
                    onClick = { onSort(ReviewSortOption.None) },
                    border = pillBorder,
                    contentPadding = PaddingValues(start = 14.dp, end = 10.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
    /** False hides everyone else's scores and the average (see ScoreReveal). */
    revealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val work = review.work
    val needsMyScore = currentUserId != null && review.scoreOf(currentUserId) == null
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Box {
            Poster(work.imageUrl, work.title, Modifier.fillMaxWidth())
            review.average?.let { avg ->
                // Cards never reveal on tap (same as web): that's the detail sheet's job.
                ScoreBadge(
                    value = avg.takeUnless { ScoreReveal.isHidden(null, currentUserId, revealed) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
            if (needsMyScore) {
                Text(
                    "Rate it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(work.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        work.addedDate?.let {
            Text(
                it.formatShort(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val scored = members.mapNotNull { m -> review.scores[m.id]?.let { m to it.value } }
        if (scored.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                scored.forEach { (member, value) ->
                    MemberScoreChip(
                        member,
                        value.takeUnless { ScoreReveal.isHidden(member.id, currentUserId, revealed) },
                        isMe = member.id == currentUserId,
                    )
                }
            }
        }
    }
}

/**
 * A work still on its way onto the reviews list: there's nothing to score or
 * open until the refetch replaces it, so it isn't selectable.
 */
@Composable
private fun PendingReviewCard(work: Work, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "Adding ${work.title}" }) {
        Box(contentAlignment = Alignment.Center) {
            Poster(work.imageUrl, work.title, Modifier.fillMaxWidth().alpha(0.5f))
            CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(work.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Adding…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MemberScoreChip(member: Member, value: Double?, isMe: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (isMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Avatar(member.name, member.imageUrl, size = 18.dp)
        if (value == null) {
            HiddenScoreIcon(Modifier.size(14.dp))
        } else {
            Text(
                ScoreScale.format(value),
                style = MaterialTheme.typography.labelMedium,
                color = if (isMe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Placeholder for a score kept hidden until you score the work or reveal it. */
@Composable
fun HiddenScoreIcon(modifier: Modifier = Modifier) {
    Icon(
        Icons.Outlined.VisibilityOff,
        contentDescription = "Hidden score",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Gold star + club average (null while hidden), legible over any poster. */
@Composable
fun ScoreBadge(value: Double?, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
            .padding(horizontal = if (large) 10.dp else 7.dp, vertical = if (large) 6.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(if (large) 20.dp else 14.dp),
        )
        if (value == null) {
            HiddenScoreIcon(Modifier.size(if (large) 20.dp else 14.dp))
        } else {
            Text(
                ScoreScale.format(value),
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
