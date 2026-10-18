package cobresun.movieclub.app.lists.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.GridSkeleton
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.member.domain.Member
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class SharedListState(
    val clubName: String = "",
    val listTitle: String = "List",
    val members: List<Member> = emptyList(),
    /** The list's items, its Up Next one first (web `SharedListView`). */
    val items: AsyncResult<List<Work>> = AsyncResult.Loading,
    val upNextId: String? = null,
)

/** Loads a shared list for a club the user may not be in; the endpoints are public. */
class SharedListViewModel(
    private val clubSlug: String,
    private val listId: String,
    private val listsRepository: ListsRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SharedListState(clubName = clubSlug))
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            clubRepository.getClubPreview(clubSlug).onSuccess { club -> _state.update { it.copy(clubName = club.name) } }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { m -> _state.update { it.copy(members = m) } }
        }
        viewModelScope.launch {
            listsRepository.getLists(clubSlug).onSuccess { lists ->
                lists.firstOrNull { it.id == listId }?.let { list -> _state.update { it.copy(listTitle = list.title) } }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(items = AsyncResult.Loading) }
            val upNext = async { (listsRepository.getUpNext(clubSlug) as? Result.Success)?.data }
            when (val items = listsRepository.getItems(clubSlug, listId)) {
                is Result.Success -> {
                    val next = upNext.await()
                    val sorted = items.data.firstOrNull { it.id == next }
                        ?.let { first -> listOf(first) + items.data.filter { it.id != first.id } }
                        ?: items.data
                    _state.update { it.copy(items = AsyncResult.Success(sorted), upNextId = next) }
                }
                is Result.Error -> _state.update { it.copy(items = AsyncResult.Error(dataError = items.error)) }
            }
        }
    }
}

@Composable
fun SharedListScreenRoot(
    clubSlug: String,
    listId: String,
    onBack: () -> Unit,
    viewModel: SharedListViewModel = koinViewModel { parametersOf(clubSlug, listId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SharedListScreen(state, onBack = onBack, onRetry = viewModel::load)
}

/** Web `SharedListView`: a club's list, read-only, with who added each item. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedListScreen(state: SharedListState, onBack: () -> Unit, onRetry: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.clubName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(state.listTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when (val items = state.items) {
                is AsyncResult.Loading -> GridSkeleton(Modifier.padding(horizontal = 16.dp))
                is AsyncResult.Error -> ErrorState(
                    message = (items.dataError as? DataError.Remote)?.toUserMessage("load this list") ?: "Failed to load list",
                    onRetry = onRetry,
                )
                is AsyncResult.Success -> if (items.data.isEmpty()) {
                    EmptyState(icon = Icons.AutoMirrored.Outlined.List, title = "List is empty", message = "This list is empty.")
                } else {
                    val membersById = state.members.associateBy { it.id }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items.data, key = { it.id }) { work ->
                            ListItemCard(
                                work = work,
                                addedBy = work.addedByUserId?.let(membersById::get),
                                isUpNext = work.id == state.upNextId,
                                pulse = null,
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
