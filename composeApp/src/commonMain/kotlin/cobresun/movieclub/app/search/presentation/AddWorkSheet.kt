package cobresun.movieclub.app.search.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
                placeholder = { Text("Search for a ${workType.noun}") },
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
            AnimatedContent(
                targetState = showingSuggestions to content,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { (suggest, result) -> suggest to (result::class) },
                modifier = Modifier.weight(1f),
            ) { (suggest, result) ->
                when (result) {
                    is AsyncResult.Loading -> SearchSkeleton()
                    is AsyncResult.Error -> ErrorState(
                        message = state.errorMessage ?: "Search is unavailable right now.",
                        onRetry = viewModel::retry,
                    )

                    is AsyncResult.Success -> if (result.data.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matches",
                            message = "Try a different spelling or fewer words.",
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                            item {
                                SectionLabel(
                                    if (!suggest) "Results"
                                    else if (workType == WorkType.MOVIE) "Trending this week"
                                    else "Popular picks",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                )
                            }
                            items(result.data, key = { it.externalId }) { item ->
                                SearchResultRow(
                                    result = item,
                                    alreadyAdded = item.externalId in existingExternalIds,
                                    onClick = { onPick(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: WorkSearchResult,
    alreadyAdded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
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
        }
        if (alreadyAdded) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Already added", tint = MaterialTheme.colorScheme.secondary)
        } else {
            Icon(Icons.Default.AddCircle, contentDescription = "Add ${result.title}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SearchSkeleton() {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(6) {
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
