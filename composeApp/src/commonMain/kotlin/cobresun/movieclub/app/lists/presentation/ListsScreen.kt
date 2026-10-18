package cobresun.movieclub.app.lists.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import cobresun.movieclub.app.reviews.presentation.EmphasizedEasing
import cobresun.movieclub.app.reviews.presentation.scaleInOnEnter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.workType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.filters.presentation.SearchFilterBar

/**
 * The Lists tab, like web `WatchListView`: every list stacked in one scroll,
 * each collapsible, with its own random pick, share and add buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    state: ListsState,
    onAction: (ListsAction) -> Unit,
    onAddClick: (listId: String) -> Unit,
    hapticFeedback: HapticFeedback,
    /** "Mark as watched": move a work from a list to Reviews and go there. */
    onMarkFinished: (work: Work, fromListId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showManage by rememberSaveable { mutableStateOf(false) }
    val enteringKeys = rememberEnteringKeys(state)
    val gridState = rememberLazyGridState()
    val drag = rememberListGridDrag(
        gridState = gridState,
        haptics = hapticFeedback,
        visibleIds = { listId -> state.visibleItems(listId).map { it.id } },
        upNextId = { state.upNextWorkId },
        isPending = { workId -> workId.startsWith(PENDING_PREFIX) },
        onDrop = { listId, order, droppedId -> onAction(ListsAction.Reorder(listId, order, droppedId)) },
    )

    state.pulse?.let { pulse ->
        LaunchedEffect(pulse) {
            delay(800)
            onAction(ListsAction.PulseShown)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListsHeader(clubName = state.clubName, onManage = { showManage = true })
            when (val lists = state.lists) {
                is AsyncResult.Loading -> ListsSkeleton()
                is AsyncResult.Error -> ErrorState(
                    message = (lists.dataError as? DataError.Remote)?.toUserMessage("load your lists")
                        ?: "Couldn't load your lists.",
                    onRetry = { onAction(ListsAction.Refresh) },
                )

                is AsyncResult.Success -> when {
                    lists.data.isEmpty() -> EmptyState(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        title = "No lists yet",
                        message = "Create your first list to get started.",
                        actionLabel = "Create list",
                        onAction = { showManage = true },
                    )

                    else -> PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onAction(ListsAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 104.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp + LocalTabBarInset.current),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxSize().listGridDrag(drag),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "filter") {
                                SearchFilterBar(
                                    query = state.filterQuery,
                                    results = state.filters,
                                    onQueryChange = { onAction(ListsAction.Filter(it)) },
                                    placeholder = "Search watchlists",
                                    horizontalPadding = 0.dp,
                                )
                            }
                            lists.data.forEach { list ->
                                listSection(
                                    list = list,
                                    state = state,
                                    onAction = onAction,
                                    onAdd = { onAddClick(list.id) },
                                    onOpen = { work -> onAction(ListsAction.OpenDetail(list.id, work.id)) },
                                    enteringKeys = enteringKeys,
                                    drag = drag,
                                )
                            }
                        }
                    }
                }
            }
        }

    }

    state.shuffle?.let { shuffle ->
        RandomPickerSheet(
            candidates = shuffle.candidates,
            winner = shuffle.selected,
            noun = state.clubType.workType.noun,
            otherLists = state.listsOrEmpty.filter { it.id != shuffle.listId },
            hapticFeedback = hapticFeedback,
            onMakeUpNext = { onAction(ListsAction.SetUpNext(it)) },
            onMoveToList = { work, listId -> onAction(ListsAction.MoveWork(work, shuffle.listId, listId)) },
            onDismiss = { onAction(ListsAction.CloseShuffle) },
        )
    }

    state.detail?.let { detail ->
        state.itemsOrEmpty(detail.listId).firstOrNull { it.id == detail.workId }?.let { work ->
            ListItemDetailSheet(
                work = work,
                detail = detail,
                isUpNext = work.id == state.upNextWorkId,
                addedBy = work.addedByUserId?.let { state.members[it] },
                lists = state.listsOrEmpty,
                currentUserId = state.currentUserId,
                hapticFeedback = hapticFeedback,
                onAction = onAction,
                onMarkFinished = { onMarkFinished(it, detail.listId) },
                onDismiss = { onAction(ListsAction.CloseDetail) },
            )
        }
    }

    if (showManage) {
        ManageListsSheet(
            lists = state.listsOrEmpty,
            noun = state.clubType.workType.noun,
            onAction = onAction,
            onDismiss = { showManage = false },
        )
    }
}

/**
 * Grid keys ("listId/workId") that just appeared on a list that was already
 * showing (an add, a move in, an expand), so only those cards scale in; the
 * first load and cards scrolled into view don't animate. Read once per card,
 * at its first composition, which happens right after this composition.
 */
@Composable
private fun rememberEnteringKeys(state: ListsState): Set<String> {
    val previous = remember { arrayOfNulls<Map<String, Set<String>>>(1) }
    val current = state.listsOrEmpty.filter { !state.isCollapsed(it.id) }.mapNotNull { list ->
        (state.items(list.id) as? AsyncResult.Success)?.let { list.id to state.visibleItems(list.id).mapTo(HashSet()) { "${list.id}/${it.id}" } }
    }.toMap()
    val before = previous[0]
    val entering = if (before == null) emptySet() else current.flatMapTo(HashSet()) { (listId, keys) ->
        before[listId]?.let { keys - it }.orEmpty()
    }
    // A collapsed list counts as showing nothing, so expanding it scales its cards in.
    val collapsed = state.listsOrEmpty
        .filter { state.isCollapsed(it.id) && state.items(it.id) is AsyncResult.Success }
        .associate { it.id to emptySet<String>() }
    SideEffect { previous[0] = current + collapsed }
    return entering
}

/** Web `WatchListSkeleton`: the search bar, then two list sections with their header and posters. */
@Composable
private fun ListsSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(52.dp), MaterialTheme.shapes.extraLarge)
        repeat(2) {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShimmerBox(Modifier.size(24.dp))
                ShimmerBox(Modifier.width(140.dp).height(18.dp))
                Spacer(Modifier.weight(1f))
                repeat(3) { ShimmerBox(Modifier.size(24.dp)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                        ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ListsHeader(clubName: String, onManage: () -> Unit) {
    // The gear sits on the title row: the eyebrow row is shared with the shell's account avatar.
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
        ClubNameLabel(clubName)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lists", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onManage) {
                Icon(Icons.Outlined.Settings, contentDescription = "Manage lists")
            }
        }
    }
}

/** One list's header and, unless collapsed, its poster grid. */
private fun LazyGridScope.listSection(
    list: WorkList,
    state: ListsState,
    onAction: (ListsAction) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Work) -> Unit,
    enteringKeys: Set<String>,
    drag: ListGridDrag,
) {
    val collapsed = state.isCollapsed(list.id)
    item(span = { GridItemSpan(maxLineSpan) }, key = "header-${list.id}", contentType = "header") {
        ListSectionHeader(
            list = list,
            collapsed = collapsed,
            onToggle = { onAction(ListsAction.ToggleCollapsed(list.id)) },
            onShuffle = { onAction(ListsAction.Shuffle(list.id)) },
            onShare = { onAction(ListsAction.ShareList(list.id)) },
            onAdd = onAdd,
            modifier = Modifier.animateItem(),
        )
    }
    if (collapsed) return

    when (val items = state.items(list.id)) {
        is AsyncResult.Loading -> items(3, key = { "skeleton-${list.id}-$it" }, contentType = { "skeleton" }) {
            Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp))
            }
        }

        is AsyncResult.Error -> item(span = { GridItemSpan(maxLineSpan) }, key = "error-${list.id}") {
            Row(Modifier.animateItem(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Couldn't load this list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onAction(ListsAction.Refresh) }) { Text("Retry") }
            }
        }

        is AsyncResult.Success -> {
            val shown = state.visibleItems(list.id)
            // While a card of this list is lifted, lay the list out in the live drag order.
            val visible = if (drag.listId == list.id) {
                val byId = shown.associateBy { it.id }
                drag.order.mapNotNull { byId[it] }
            } else {
                shown
            }
            if (items.data.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty-${list.id}") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        title = "Empty list",
                        message = "Add items to this list to see them here.",
                        actionLabel = "Add a ${state.clubType.workType.noun}",
                        onAction = onAdd,
                        modifier = Modifier.animateItem(),
                    )
                }
            } else if (visible.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "nomatch-${list.id}") {
                    Text(
                        "Nothing on this list matches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem().padding(horizontal = 4.dp),
                    )
                }
            }
            items(visible, key = { cardKey(list.id, it.id) }, contentType = { "work" }) { work ->
                val key = cardKey(list.id, work.id)
                val entering = remember { key in enteringKeys }
                val lifted = drag.listId == list.id && drag.draggedId == work.id
                ListItemCard(
                    work = work,
                    addedBy = work.addedByUserId?.let { state.members[it] },
                    isUpNext = work.id == state.upNextWorkId,
                    pulse = state.pulse?.takeIf { it.workId == work.id }?.id,
                    onClick = { if (!work.isPending) onOpen(work) },
                    pending = work.isPending,
                    lifted = lifted,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(200),
                            fadeOutSpec = tween(200),
                            // The lifted card follows the finger; the others glide aside.
                            placementSpec = if (lifted) null else tween(200, easing = EmphasizedEasing),
                        )
                        .then(
                            if (lifted) {
                                Modifier.zIndex(1f).graphicsLayer {
                                    val d = drag.displacement()
                                    translationX = d.x
                                    translationY = d.y
                                }
                            } else {
                                Modifier
                            }
                        )
                        .scaleInOnEnter(entering, from = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun ListSectionHeader(
    list: WorkList,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onShuffle: () -> Unit,
    onShare: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevron by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = if (collapsed) "Expand list" else "Collapse list", onClick = onToggle)
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevron),
        )
        Spacer(Modifier.width(6.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                list.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "(${list.itemCount})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (list.itemCount > 1) {
            IconButton(onClick = onShuffle) {
                Icon(Icons.Default.Casino, contentDescription = "Random pick")
            }
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Outlined.Share, contentDescription = "Share list")
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Add to list")
        }
    }
}
