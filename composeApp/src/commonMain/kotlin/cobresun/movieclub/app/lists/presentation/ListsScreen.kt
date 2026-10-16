package cobresun.movieclub.app.lists.presentation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.workType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.components.GridSkeleton
import cobresun.movieclub.app.core.presentation.components.TextInputDialog
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.core.presentation.posterSubtitle
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.lists.domain.WorkList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    state: ListsState,
    onAction: (ListsAction) -> Unit,
    onAddClick: () -> Unit,
    hapticFeedback: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    var detailWorkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showManage by rememberSaveable { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    val workType = state.clubType.workType

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListsHeader(
                clubName = state.clubName,
                canShuffle = (state.selectedItems as? AsyncResult.Success)?.data?.isNotEmpty() == true,
                canReorder = ((state.selectedItems as? AsyncResult.Success)?.data?.size ?: 0) > 1,
                onShuffle = { onAction(ListsAction.Shuffle) },
                onReorder = { onAction(ListsAction.StartReorder) },
                onManage = { showManage = true },
                onShare = { onAction(ListsAction.ShareList) },
                canShare = state.selectedList != null,
            )
            when (val lists = state.lists) {
                is AsyncResult.Loading -> GridSkeleton()
                is AsyncResult.Error -> ErrorState(
                    message = lists.dataError.let { it as? cobresun.movieclub.app.core.domain.DataError.Remote }
                        ?.toUserMessage("load your lists") ?: "Couldn't load your lists.",
                    onRetry = { onAction(ListsAction.Refresh) },
                )

                is AsyncResult.Success -> {
                    ListPicker(
                        lists = lists.data,
                        selectedId = state.selectedListId,
                        onSelect = { onAction(ListsAction.SelectList(it)) },
                        onCreate = { showCreate = true },
                    )
                    if (state.reorderDraft != null) {
                        ReorderList(
                            items = state.reorderDraft,
                            onMove = { from, to -> onAction(ListsAction.MoveItem(from, to)) },
                            onCancel = { onAction(ListsAction.CancelReorder) },
                            onSave = { onAction(ListsAction.SaveReorder) },
                        )
                    } else if (lists.data.isEmpty()) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            title = "No lists yet",
                            message = "Create a list to start collecting ${workType.noun}s for the club.",
                            actionLabel = "Create a list",
                            onAction = { showCreate = true },
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { onAction(ListsAction.Refresh) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ListContent(
                                state = state,
                                onOpen = { detailWorkId = it.id },
                                onAddClick = onAddClick,
                                onRetry = { onAction(ListsAction.Refresh) },
                                onFilter = { onAction(ListsAction.Filter(it)) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.reorderDraft == null && state.selectedList != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = LocalTabBarInset.current).padding(20.dp),
        ) {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add ${workType.noun}") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
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

    val allItems = (state.selectedItems as? AsyncResult.Success)?.data.orEmpty()
    allItems.firstOrNull { it.id == detailWorkId }?.let { work ->
        ListItemDetailSheet(
            work = work,
            isUpNext = work.id == state.upNextWorkId,
            addedByName = work.addedByUserId?.let { state.memberNames[it] },
            currentListId = state.selectedListId,
            lists = (state.lists as? AsyncResult.Success)?.data.orEmpty(),
            onAction = onAction,
            onDismiss = { detailWorkId = null },
        )
    }

    if (showManage) {
        ManageListsSheet(
            lists = (state.lists as? AsyncResult.Success)?.data.orEmpty(),
            onAction = onAction,
            onDismiss = { showManage = false },
        )
    }

    if (showCreate) {
        TextInputDialog(
            title = "New list",
            initialValue = "",
            confirmLabel = "Create",
            placeholder = "e.g. Halloween picks",
            onConfirm = { onAction(ListsAction.CreateList(it)) },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun ListsHeader(
    clubName: String,
    canShuffle: Boolean,
    canReorder: Boolean,
    onShuffle: () -> Unit,
    onReorder: () -> Unit,
    onManage: () -> Unit,
    onShare: () -> Unit,
    canShare: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            ClubNameLabel(clubName)
            Text("Lists", style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(onClick = onShuffle, enabled = canShuffle) {
            Icon(Icons.Default.Casino, contentDescription = "Pick at random")
        }
        IconButton(onClick = onReorder, enabled = canReorder) {
            Icon(Icons.Outlined.SwapVert, contentDescription = "Reorder items")
        }
        IconButton(onClick = onShare, enabled = canShare) {
            Icon(Icons.Outlined.Share, contentDescription = "Share list")
        }
        IconButton(onClick = onManage) {
            Icon(Icons.Outlined.EditNote, contentDescription = "Manage lists")
        }
    }
}

@Composable
private fun ListPicker(
    lists: List<WorkList>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        items(lists, key = { it.id }) { list ->
            val selected = list.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(list.id) },
                label = {
                    Text(list.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        list.itemCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = null,
            )
        }
        item(key = "new") {
            FilterChip(
                selected = false,
                onClick = onCreate,
                label = { Text("New list") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.secondary,
                    iconColor = MaterialTheme.colorScheme.secondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun ListContent(
    state: ListsState,
    onOpen: (Work) -> Unit,
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    onFilter: (String) -> Unit,
) {
    val workType = state.clubType.workType
    Crossfade(targetState = state.selectedItems::class, label = "list-content") { kind ->
        when (kind) {
            AsyncResult.Loading::class -> GridSkeleton()
            AsyncResult.Error::class -> ErrorState(
                message = "Couldn't load this list. Check your connection and try again.",
                onRetry = onRetry,
            )

            else -> {
                val all = (state.selectedItems as? AsyncResult.Success)?.data.orEmpty()
                val upNext = state.upNext?.takeIf { state.filter.isBlank() }
                val items = state.visibleItems
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp + LocalTabBarInset.current),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (all.size > 4) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "filter") {
                            FilterField(query = state.filter, onQueryChange = onFilter, noun = workType.noun)
                        }
                    }
                    if (upNext != null) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "upnext-${upNext.id}") {
                            UpNextCard(work = upNext, onClick = { onOpen(upNext) })
                        }
                    }
                    if (all.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                            EmptyState(
                                icon = Icons.Outlined.VideoLibrary,
                                title = "Nothing here yet",
                                message = "Search for a ${workType.noun} and add it to ${state.selectedList?.title ?: "this list"}.",
                                actionLabel = "Add a ${workType.noun}",
                                onAction = onAddClick,
                            )
                        }
                    } else if (items.isEmpty() && state.filter.isNotBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "nomatch") {
                            EmptyState(
                                icon = Icons.Outlined.SearchOff,
                                title = "No matches",
                                message = "Nothing on this list matches \"${state.filter}\".",
                            )
                        }
                    } else if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "count") {
                            Text(
                                text = "${items.size} ${if (upNext != null) "more " else ""}${workType.noun}${if (items.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(items, key = { it.id }) { work ->
                        PosterTile(work = work, onClick = { onOpen(work) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
fun PosterTile(work: Work, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Poster(imageUrl = work.imageUrl, title = work.title, modifier = Modifier.fillMaxWidth())
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
