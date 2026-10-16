package cobresun.movieclub.app.reviews.presentation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import cobresun.movieclub.app.core.presentation.finishedVerb
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ScoreScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    state: ReviewsState,
    onAction: (ReviewsAction) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workType = state.clubType.workType
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
                    SearchAndSort(state = state, onAction = onAction)
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onAction(ReviewsAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val visible = state.visibleReviews
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (reviews.data.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyState(
                                        icon = Icons.Outlined.RateReview,
                                        title = "No reviews yet",
                                        message = "Once the club has ${workType.finishedVerb} something, add it here so everyone can score it.",
                                        actionLabel = "Add a review",
                                        onAction = onAddClick,
                                    )
                                }
                            } else if (visible.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyState(
                                        icon = Icons.Outlined.SearchOff,
                                        title = "No matches",
                                        message = "No reviewed ${workType.noun}s match \"${state.query}\".",
                                    )
                                }
                            }
                            items(visible, key = { it.work.id }) { review ->
                                ReviewCard(
                                    review = review,
                                    members = state.members,
                                    currentUserId = state.currentUserId,
                                    onClick = { onAction(ReviewsAction.OpenDetail(review.work.id)) },
                                    modifier = Modifier.animateItem(),
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
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
                onAction = onAction,
                onDismiss = { onAction(ReviewsAction.CloseDetail) },
            )
        }
    }
}

@Composable
private fun SearchAndSort(state: ReviewsState, onAction: (ReviewsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            value = state.query,
            onValueChange = { onAction(ReviewsAction.Search(it)) },
            placeholder = { Text("Search titles, people, genres") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onAction(ReviewsAction.Search("")) }) {
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        SortChips(
            sortState = state.sortState,
            members = state.members,
            onSort = { onAction(ReviewsAction.Sort(it)) },
            onToggleDirection = { onAction(ReviewsAction.ToggleSortDirection) },
        )
    }
}

@Composable
private fun SortChips(
    sortState: ReviewSortState,
    members: List<Member>,
    onSort: (ReviewSortOption) -> Unit,
    onToggleDirection: () -> Unit,
) {
    data class Opt(val option: ReviewSortOption, val label: String, val member: Member? = null)
    val options = listOf(
        Opt(ReviewSortOption.DateReviewed, "Date"),
        Opt(ReviewSortOption.AverageScore, "Average"),
        Opt(ReviewSortOption.Title, "Title"),
    ) + members.map { Opt(ReviewSortOption.MemberScore(it.id), it.name, it) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sortState.isActive) {
            item(key = "dir") {
                IconButton(onClick = onToggleDirection, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (sortState.descending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (sortState.descending) "Descending" else "Ascending",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        items(options, key = { it.label + it.option.toString() }) { opt ->
            val selected = sortState.option == opt.option
            FilterChip(
                selected = selected,
                onClick = { onSort(opt.option) },
                label = { Text(opt.label, maxLines = 1) },
                leadingIcon = opt.member?.let { m -> { Avatar(m.name, m.imageUrl, size = 20.dp) } },
                shape = MaterialTheme.shapes.extraLarge,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}

@Composable
private fun ReviewCard(
    review: Review,
    members: List<Member>,
    currentUserId: String?,
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
                ScoreBadge(
                    value = avg,
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
                    MemberScoreChip(member, value, isMe = member.id == currentUserId)
                }
            }
        }
    }
}

@Composable
fun MemberScoreChip(member: Member, value: Double, isMe: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (isMe) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Avatar(member.name, member.imageUrl, size = 18.dp)
        Text(
            ScoreScale.format(value),
            style = MaterialTheme.typography.labelMedium,
            color = if (isMe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Gold star + club average, legible over any poster. */
@Composable
fun ScoreBadge(value: Double, modifier: Modifier = Modifier, large: Boolean = false) {
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
        Text(
            ScoreScale.format(value),
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
