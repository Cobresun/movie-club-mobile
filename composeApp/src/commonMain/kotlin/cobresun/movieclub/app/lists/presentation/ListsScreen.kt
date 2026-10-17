package cobresun.movieclub.app.lists.presentation

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import cobresun.movieclub.app.core.presentation.components.GridSkeleton
import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.core.presentation.posterSubtitle
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.lists.domain.WorkList

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
    modifier: Modifier = Modifier,
) {
    var detailListId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailWorkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showManage by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListsHeader(clubName = state.clubName, onManage = { showManage = true })
            when (val lists = state.lists) {
                is AsyncResult.Loading -> GridSkeleton()
                is AsyncResult.Error -> ErrorState(
                    message = (lists.dataError as? DataError.Remote)?.toUserMessage("load your lists")
                        ?: "Couldn't load your lists.",
                    onRetry = { onAction(ListsAction.Refresh) },
                )

                is AsyncResult.Success -> when {
                    state.reorderDraft != null -> ReorderList(
                        items = state.reorderDraft.items,
                        onMove = { from, to -> onAction(ListsAction.MoveItem(from, to)) },
                        onCancel = { onAction(ListsAction.CancelReorder) },
                        onSave = { onAction(ListsAction.SaveReorder) },
                    )

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
                            columns = GridCells.Adaptive(minSize = 104.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp + LocalTabBarInset.current),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "filter") {
                                FilterField(
                                    query = state.filter,
                                    onQueryChange = { onAction(ListsAction.Filter(it)) },
                                    noun = state.clubType.workType.noun,
                                )
                            }
                            lists.data.forEach { list ->
                                listSection(
                                    list = list,
                                    state = state,
                                    onAction = onAction,
                                    onAdd = { onAddClick(list.id) },
                                    onOpen = { work ->
                                        detailListId = list.id
                                        detailWorkId = work.id
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        state.shuffle?.let { shuffle ->
            ShuffleOverlay(
                candidates = shuffle.candidates,
                selected = shuffle.selected,
                onFinished = { onAction(ListsAction.ShuffleFinished) },
                hapticFeedback = hapticFeedback,
            )
        }
    }

    val detailList = detailListId
    if (detailList != null) {
        state.itemsOrEmpty(detailList).firstOrNull { it.id == detailWorkId }?.let { work ->
            ListItemDetailSheet(
                work = work,
                isUpNext = work.id == state.upNextWorkId,
                addedByName = work.addedByUserId?.let { state.members[it]?.name },
                currentListId = detailList,
                lists = state.listsOrEmpty,
                onAction = onAction,
                onDismiss = {
                    detailListId = null
                    detailWorkId = null
                },
            )
        }
    }

    if (showManage) {
        ManageListsSheet(
            lists = state.listsOrEmpty,
            onAction = onAction,
            onDismiss = { showManage = false },
        )
    }
}

@Composable
private fun ListsHeader(clubName: String, onManage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            ClubNameLabel(clubName)
            Text("Lists", style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onManage) {
            Icon(Icons.Outlined.Settings, contentDescription = "Manage lists")
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
) {
    val collapsed = state.isCollapsed(list.id)
    item(span = { GridItemSpan(maxLineSpan) }, key = "header-${list.id}", contentType = "header") {
        ListSectionHeader(
            list = list,
            collapsed = collapsed,
            onToggle = { onAction(ListsAction.ToggleCollapsed(list.id)) },
            onShuffle = { onAction(ListsAction.Shuffle(list.id)) },
            onReorder = { onAction(ListsAction.StartReorder(list.id)) },
            onShare = { onAction(ListsAction.ShareList(list.id)) },
            onAdd = onAdd,
            modifier = Modifier.animateItem(),
        )
    }
    if (collapsed) return

    when (val items = state.items(list.id)) {
        is AsyncResult.Loading -> items(3, key = { "skeleton-${list.id}-$it" }, contentType = { "skeleton" }) {
            Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().height(156.dp))
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
            val visible = state.visibleItems(list.id)
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
                        "Nothing on this list matches \"${state.filter.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem().padding(horizontal = 4.dp),
                    )
                }
            }
            items(visible, key = { "${list.id}/${it.id}" }, contentType = { "work" }) { work ->
                PosterTile(
                    work = work,
                    highlighted = work.id == state.upNextWorkId,
                    onClick = { onOpen(work) },
                    modifier = Modifier.animateItem(),
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
    onReorder: () -> Unit,
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
        Spacer(Modifier.weight(1f))
        if (list.itemCount > 1) {
            IconButton(onClick = onShuffle) {
                Icon(Icons.Default.Casino, contentDescription = "Random pick")
            }
            IconButton(onClick = onReorder) {
                Icon(Icons.Outlined.SwapVert, contentDescription = "Reorder items")
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

@Composable
fun PosterTile(work: Work, onClick: () -> Unit, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Poster(imageUrl = work.imageUrl, title = work.title, modifier = Modifier.fillMaxWidth(), highlighted = highlighted)
        Spacer(Modifier.height(8.dp))
        Text(
            work.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        work.posterSubtitle()?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FilterField(query: String, onQueryChange: (String) -> Unit, noun: String) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Filter ${noun}s by title, person or genre") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear filter") }
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
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReorderList(
    items: List<Work>,
    onMove: (Int, Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Text(
                "Reorder",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Button(onClick = onSave) { Text("Save") }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp + LocalTabBarInset.current),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items, key = { _, w -> w.id }) { index, work ->
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center,
                    )
                    Poster(work.imageUrl, work.title, Modifier.width(40.dp), MaterialTheme.shapes.extraSmall)
                    Text(work.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${work.title} up")
                    }
                    IconButton(onClick = { onMove(index, index + 1) }, enabled = index < items.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${work.title} down")
                    }
                }
            }
        }
    }
}
