package cobresun.movieclub.app.search.presentation

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
 * Full-height sheet to find a movie/book and add it somewhere.
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
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(workType) { viewModel.start(workType) }

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
                placeholder = { Text(if (fromLists != null) "Filter your lists or search" else "Search for a ${workType.noun}") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(16.dp))

            val showingSuggestions = state.results == null
            val content = state.results ?: state.suggestions
            val lists = fromLists
            var listFilter by remember { mutableStateOf<String?>(null) }
            var showAllPicks by remember { mutableStateOf(false) }
            val listItems = (lists?.items as? AsyncResult.Success)?.data.orEmpty()
            val listTitles = remember(listItems) { listItems.map { it.listTitle }.distinct() }
            // Like the web prompt: the search text also filters the list picks by title.
            val matchingPicks = listItems.filter {
                (listFilter == null || it.listTitle == listFilter) &&
                    it.title.contains(state.query.trim(), ignoreCase = true)
            }
            val visiblePicks = if (showAllPicks || state.query.isNotBlank()) matchingPicks else matchingPicks.take(COLLAPSED_PICKS)

            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (lists != null) {
                    when (lists.items) {
                        is AsyncResult.Loading -> item(key = "lists-loading") {
                            Column {
                                SectionLabel(FROM_LISTS, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                                SearchSkeleton(rows = 2)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        is AsyncResult.Error -> Unit // Search still works; lists are a shortcut.
                        is AsyncResult.Success -> if (matchingPicks.isNotEmpty() || (listFilter != null && state.query.isBlank())) {
                            item(key = "lists-header") {
                                Column(Modifier.animateItem()) {
                                    SectionLabel(FROM_LISTS, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                                    if (listTitles.size > 1) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
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
                            items(visiblePicks, key = { "pick-" + it.key }) { pick ->
                                ListPickRow(pick, Modifier.animateItem()) { lists.onPick(pick) }
                            }
                            if (visiblePicks.size < matchingPicks.size) {
                                item(key = "lists-more") {
                                    TextButton(
                                        onClick = { showAllPicks = true },
                                        modifier = Modifier.padding(horizontal = 8.dp).animateItem(),
                                    ) { Text("Show all ${matchingPicks.size}") }
                                }
                            }
                            item(key = "lists-gap") { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }

                when (content) {
                    is AsyncResult.Loading -> item(key = "search-loading") { SearchSkeleton() }
                    is AsyncResult.Error -> item(key = "search-error") {
                        ErrorState(
                            message = state.errorMessage ?: "Search is unavailable right now.",
                            onRetry = viewModel::retry,
                        )
                    }

                    is AsyncResult.Success -> if (content.data.isEmpty()) {
                        if (matchingPicks.isEmpty()) item(key = "search-empty") {
                            EmptyState(
                                icon = Icons.Outlined.SearchOff,
                                title = "No matches",
                                message = "Try a different spelling or fewer words.",
                            )
                        }
                    } else {
                        item(key = "search-header-$showingSuggestions") {
                            SectionLabel(
                                if (!showingSuggestions) "Search results"
                                else if (workType == WorkType.MOVIE) "Trending this week"
                                else "Popular picks",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).animateItem(),
                            )
                        }
                        items(content.data, key = { "search-" + it.externalId }) { item ->
                            SearchResultRow(
                                result = item,
                                alreadyAdded = item.externalId in existingExternalIds,
                                status = statusLabels[item.externalId],
                                onClick = { onPick(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
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

@Composable
private fun SearchResultRow(
    result: WorkSearchResult,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Poster(imageUrl = result.imageUrl, title = result.title, modifier = Modifier.width(52.dp), shape = MaterialTheme.shapes.extraSmall)
        Column(Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            result.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (alreadyAdded) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Already added", tint = MaterialTheme.colorScheme.secondary)
        } else {
            Icon(Icons.Default.AddCircle, contentDescription = "Add ${result.title}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SearchSkeleton(rows: Int = 6) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
