package cobresun.movieclub.app.search.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.noun
import cobresun.movieclub.app.search.domain.WorkSearchResult
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-height sheet to find a movie/book and add it somewhere (web
 * `AddWorkModal` / `AddReviewPrompt` around `WorkSearchPrompt`). Adding to a
 * list offers browse tabs; typing filters what's shown and searches too.
 *
 * @param title e.g. "Add to Watch List"
 * @param existingExternalIds works already at the destination (shown as added)
 * @param statusLabels where other results already live in the club (e.g. "Reviewed")
 * @param fromLists works already on the club's lists to offer before search results
 * @param onPick called with the chosen result; the caller performs the add and dismisses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkSheet(
    title: String,
    workType: WorkType,
    existingExternalIds: Set<String>,
    onPick: (WorkSearchResult) -> Unit,
    onDismiss: () -> Unit,
    /** Optional "From your lists" picks shown above search (Add review). */
    fromLists: ListPickSection? = null,
    statusLabels: Map<String, String> = emptyMap(),
    viewModel: WorkSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    LaunchedEffect(workType) { viewModel.start(workType, browsing = fromLists == null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(if (fromLists != null) "Filter your lists or search" else "Type to filter or search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            if (state.tabs.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    items(state.tabs, key = { it.key }) { tab ->
                        TabPill(
                            label = tab.label,
                            selected = tab.key == state.selectedTab,
                            onClick = { viewModel.selectTab(tab.key) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            val query = state.query.trim()
            val lists = fromLists
            var listFilter by remember { mutableStateOf<String?>(null) }
            var showAllPicks by remember { mutableStateOf(false) }
            val listItems = (lists?.items as? AsyncResult.Success)?.data.orEmpty()
            val listTitles = remember(listItems) { listItems.map { it.listTitle }.distinct() }
            // Like the web prompt: the search text also filters the list picks by title.
            val matchingPicks = listItems.filter {
                (listFilter == null || it.listTitle == listFilter) && it.title.contains(query, ignoreCase = true)
            }
            val visiblePicks = if (showAllPicks || query.isNotEmpty()) matchingPicks else matchingPicks.take(COLLAPSED_PICKS)

            val browse = state.selectedBrowse
            val browseItems = (browse?.results as? AsyncResult.Success)?.data.orEmpty()
                .filter { it.title.contains(query, ignoreCase = true) }
            val searchItems = (state.results as? AsyncResult.Success)?.data.orEmpty()
            val loadingSearch = state.results is AsyncResult.Loading
            val loadingBrowse = browse?.results is AsyncResult.Loading

            // Infinite scroll: fetch the next page as the end of the grid comes into view.
            val canLoadMore = query.isEmpty() && browse?.nextPage != null && !browse.isLoadingMore
            LaunchedEffect(gridState, canLoadMore) {
                if (!canLoadMore) return@LaunchedEffect
                snapshotFlow {
                    val info = gridState.layoutInfo
                    (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 6
                }.filter { it }.first()
                viewModel.loadMore()
            }

            val stagger = rememberStagger()
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (lists != null) {
                    when (lists.items) {
                        is AsyncResult.Loading -> fullSpan("lists-loading") {
                            Column {
                                SectionLabel(FROM_LISTS, Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                                SearchSkeleton(rows = 2)
                            }
                        }
                        is AsyncResult.Error -> Unit // Search still works; lists are a shortcut.
                        is AsyncResult.Success -> if (matchingPicks.isNotEmpty() || (listFilter != null && query.isEmpty())) {
                            fullSpan("lists-header") {
                                Column(Modifier.animateItem()) {
                                    SectionLabel(FROM_LISTS, Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                                    if (listTitles.size > 1) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            item {
                                                FilterChip(selected = listFilter == null, onClick = { listFilter = null }, label = { Text("All") })
                                            }
                                            items(listTitles) { title ->
                                                FilterChip(
                                                    selected = listFilter == title,
                                                    onClick = { listFilter = if (listFilter == title) null else title },
                                                    label = { Text(title) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            visiblePicks.forEach { pick ->
                                fullSpan("pick-" + pick.key) { ListPickRow(pick, Modifier.animateItem()) { lists.onPick(pick) } }
                            }
                            if (visiblePicks.size < matchingPicks.size) {
                                fullSpan("lists-more") {
                                    TextButton(onClick = { showAllPicks = true }, modifier = Modifier.animateItem()) {
                                        Text("Show all ${matchingPicks.size}")
                                    }
                                }
                            }
                        }
                    }
                }

                // The selected browse tab, filtered by what's typed.
                if (browse != null) {
                    when (browse.results) {
                        is AsyncResult.Loading -> fullSpan("browse-loading") { CardSkeleton() }
                        is AsyncResult.Error -> if (query.isEmpty()) fullSpan("browse-error") {
                            ErrorState(
                                message = state.errorMessage ?: "Couldn't load ${state.selectedTabLabel ?: "suggestions"}.",
                                onRetry = viewModel::retry,
                            )
                        }
                        is AsyncResult.Success -> if (browseItems.isNotEmpty()) {
                            fullSpan("browse-header-${state.selectedTab}") {
                                SectionLabel(state.selectedTabLabel.orEmpty(), Modifier.padding(horizontal = 4.dp))
                            }
                            itemsIndexed(browseItems, key = { _, it -> "browse-${state.selectedTab}-${it.externalId}" }) { index, item ->
                                SearchCard(
                                    result = item,
                                    alreadyAdded = item.externalId in existingExternalIds,
                                    status = statusLabels[item.externalId],
                                    onClick = { onPick(item) },
                                    modifier = stagger.modifier("browse-${state.selectedTab}-${item.externalId}", index, browse.pageStart(index)),
                                )
                            }
                            if (query.isEmpty() && browse.isLoadingMore) fullSpan("browse-more") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }

                // Search results for what's typed.
                if (query.isNotEmpty()) {
                    when (val results = state.results) {
                        is AsyncResult.Loading -> fullSpan("search-loading") { CardSkeleton() }
                        is AsyncResult.Error -> fullSpan("search-error") {
                            ErrorState(
                                message = state.errorMessage ?: "Search is unavailable right now.",
                                onRetry = viewModel::retry,
                            )
                        }
                        is AsyncResult.Success -> if (results.data.isNotEmpty()) {
                            fullSpan("search-header") { SectionLabel("Search", Modifier.padding(horizontal = 4.dp)) }
                            itemsIndexed(searchItems, key = { _, it -> "search-" + it.externalId }) { index, item ->
                                SearchCard(
                                    result = item,
                                    alreadyAdded = item.externalId in existingExternalIds,
                                    status = statusLabels[item.externalId],
                                    onClick = { onPick(item) },
                                    modifier = stagger.modifier("search-$query-${item.externalId}", index, 0),
                                )
                            }
                        }
                        null -> Unit
                    }
                }

                val noResults = query.isNotEmpty() && !loadingSearch && state.results !is AsyncResult.Error &&
                    matchingPicks.isEmpty() && browseItems.isEmpty() && searchItems.isEmpty()
                if (noResults) fullSpan("no-results") {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No matches",
                        message = "Sorry, your search did not return any results",
                    )
                }
                // Nothing on screen yet: the club type's search hint (web `searchHint`).
                val nothingShown = query.isEmpty() && !loadingBrowse && browse?.results !is AsyncResult.Error &&
                    browseItems.isEmpty() && matchingPicks.isEmpty() && lists?.items !is AsyncResult.Loading
                if (nothingShown) fullSpan("hint") {
                    EmptyState(
                        icon = if (workType == WorkType.BOOK) Icons.AutoMirrored.Outlined.MenuBook else Icons.Outlined.Movie,
                        title = "Find a ${workType.noun}",
                        message = "Search for a ${workType.noun} to add.",
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.fullSpan(key: String, content: @Composable LazyGridItemScope.() -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Index of the first result on [index]'s page, so a new page staggers from its own start. */
private fun BrowseState.pageStart(index: Int): Int = (index / PAGE_SIZE) * PAGE_SIZE

private const val PAGE_SIZE = 20

/**
 * Web `result-enter`: cards fade in rising 8dp, 25ms apart (capped at 8 steps).
 * Only cards new to the sheet animate; ones scrolled back into view don't.
 */
private class Stagger {
    val seen = mutableSetOf<String>()

    fun modifier(key: String, index: Int, base: Int): Modifier =
        if (key in seen) Modifier else {
            seen += key
            Modifier.riseIn(delayMillis = (index - base).coerceIn(0, 8) * 25)
        }
}

@Composable
private fun rememberStagger(): Stagger = remember { Stagger() }

private fun Modifier.riseIn(delayMillis: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(200, delayMillis)) }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "tab",
    )
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

private const val FROM_LISTS = "From your lists"
private const val COLLAPSED_PICKS = 5

/** A work already on one of the club's lists, offered in the Add review sheet. */
data class ListPick(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val listTitle: String,
    val isUpNext: Boolean,
)

/** The "From your lists" section: [items] to offer and what picking one does. */
class ListPickSection(
    val items: AsyncResult<List<ListPick>>,
    val onPick: (ListPick) -> Unit,
)

@Composable
private fun ListPickRow(pick: ListPick, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Move to reviews", onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Poster(imageUrl = pick.imageUrl, title = pick.title, modifier = Modifier.width(52.dp), shape = MaterialTheme.shapes.extraSmall)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pick.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            pick.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                if (pick.isUpNext) "Up Next · ${pick.listTitle}" else pick.listTitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (pick.isUpNext) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        (if (pick.isUpNext) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.DriveFileMove,
            contentDescription = "Move ${pick.title} to reviews",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Web `WorkSearchCard`: poster, title, subtitle and why it's suggested; added works show a check. */
@Composable
private fun SearchCard(
    result: WorkSearchResult,
    alreadyAdded: Boolean,
    status: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = !alreadyAdded, onClickLabel = "Add ${result.title}", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Poster(imageUrl = result.imageUrl, title = result.title, modifier = Modifier.fillMaxWidth())
            if (alreadyAdded) {
                Box(
                    Modifier.matchParentSize().clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Already added",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            result.title,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        result.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        result.reason?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (!alreadyAdded && status != null) {
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Web `WorkGridSkeleton`: a few poster placeholders while a section loads. */
@Composable
private fun CardSkeleton() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), MaterialTheme.shapes.small)
                ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp))
            }
        }
    }
}

@Composable
private fun SearchSkeleton(rows: Int = 6) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(Modifier.width(52.dp).height(78.dp), MaterialTheme.shapes.extraSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(Modifier.width(180.dp).height(16.dp))
                    ShimmerBox(Modifier.width(90.dp).height(12.dp))
                }
            }
        }
    }
}
