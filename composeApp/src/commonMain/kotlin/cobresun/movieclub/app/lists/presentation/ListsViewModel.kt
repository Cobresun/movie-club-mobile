package cobresun.movieclub.app.lists.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.matchesQuery
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.isRetryable
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import cobresun.movieclub.app.core.domain.ShareLinks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListsState(
    val clubType: ClubType = ClubType.MOVIE,
    val clubName: String = "",
    val lists: AsyncResult<List<WorkList>> = AsyncResult.Loading,
    val selectedListId: String? = null,
    val itemsByList: Map<String, AsyncResult<List<Work>>> = emptyMap(),
    val upNextWorkId: String? = null,
    val filter: String = "",
    val isRefreshing: Boolean = false,
    /** userId -> display name, for "Added by". */
    val memberNames: Map<String, String> = emptyMap(),
    /** Non-null while the random picker animation runs. */
    val shuffle: ShuffleState? = null,
    /** Non-null while reordering the selected list. */
    val reorderDraft: List<Work>? = null,
    /** Every item across the user lists, for picking a review in the Add review sheet. */
    val allItems: AsyncResult<List<ListedWork>> = AsyncResult.Loading,
) {
    /** [allItems] with the Up Next work first, then list order. */
    val pickableItems: List<ListedWork>
        get() = (allItems as? AsyncResult.Success)?.data.orEmpty()
            .sortedByDescending { it.work.id == upNextWorkId }

    val selectedList: WorkList?
        get() = (lists as? AsyncResult.Success)?.data?.firstOrNull { it.id == selectedListId }

    val selectedItems: AsyncResult<List<Work>>
        get() = selectedListId?.let { itemsByList[it] } ?: AsyncResult.Loading

    /** The Up Next work if it lives on the selected list. */
    val upNext: Work?
        get() = (selectedItems as? AsyncResult.Success)?.data?.firstOrNull { it.id == upNextWorkId }

    /** Selected list's items, minus Up Next, filtered by [filter]. */
    val visibleItems: List<Work>
        get() = (selectedItems as? AsyncResult.Success)?.data.orEmpty()
            .filter { it.id != upNextWorkId && it.matchesQuery(filter) }
}

data class ShuffleState(val candidates: List<Work>, val selected: Work)

sealed interface ListsEvent {
    data class Message(val message: UiMessage) : ListsEvent

    /** A work left the lists for the reviews tab. */
    data class MovedToReviews(val title: String) : ListsEvent

    /** Open the system share sheet for a public link. */
    data class Share(val url: String, val title: String) : ListsEvent
}

sealed interface ListsAction {
    data class SelectList(val listId: String) : ListsAction
    data object Refresh : ListsAction
    data class Filter(val query: String) : ListsAction

    data class CreateList(val title: String) : ListsAction
    data class RenameList(val listId: String, val title: String) : ListsAction
    data class DeleteList(val listId: String) : ListsAction
    data class MoveList(val listId: String, val offset: Int) : ListsAction

    data class AddWork(val work: NewWork) : ListsAction
    data class RemoveWork(val work: Work) : ListsAction
    data class SetUpNext(val work: Work) : ListsAction
    data object ClearUpNext : ListsAction
    data class MoveWork(val work: Work, val destinationListId: String) : ListsAction
    /** Moves [work] from [fromListId] (default: the selected list) into reviews. */
    data class MoveToReviews(val work: Work, val fromListId: String? = null) : ListsAction

    /** (Re)loads [ListsState.allItems] for the Add review sheet. */
    data object LoadAllItems : ListsAction

    data object StartReorder : ListsAction
    data class MoveItem(val from: Int, val to: Int) : ListsAction
    data object SaveReorder : ListsAction
    data object CancelReorder : ListsAction

    data object Shuffle : ListsAction
    data object ShareList : ListsAction
    data object ShuffleFinished : ListsAction
}

class ListsViewModel(
    savedStateHandle: SavedStateHandle,
    private val listsRepository: ListsRepository,
    private val memberRepository: MemberRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    private val _state = MutableStateFlow(ListsState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ListsEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        loadClubContext()
        load(initial = true)
    }

    fun onAction(action: ListsAction) {
        when (action) {
            is ListsAction.SelectList -> selectList(action.listId)
            ListsAction.Refresh -> {
                if (_state.value.clubName.isBlank()) loadClubContext()
                load(initial = false)
            }
            is ListsAction.Filter -> _state.update { it.copy(filter = action.query) }
            is ListsAction.CreateList -> createList(action.title)
            is ListsAction.RenameList -> renameList(action.listId, action.title)
            is ListsAction.DeleteList -> deleteList(action.listId)
            is ListsAction.MoveList -> moveList(action.listId, action.offset)
            is ListsAction.AddWork -> addWork(action.work)
            is ListsAction.RemoveWork -> removeWork(action.work)
            is ListsAction.SetUpNext -> setUpNext(action.work.id, "${action.work.title} is up next")
            ListsAction.ClearUpNext -> setUpNext(null, "Up Next cleared")
            is ListsAction.MoveWork -> moveWork(action.work, action.destinationListId)
            is ListsAction.MoveToReviews -> moveToReviews(action.work, action.fromListId)
            ListsAction.LoadAllItems -> loadAllItems()
            ListsAction.StartReorder -> _state.update {
                it.copy(reorderDraft = (it.selectedItems as? AsyncResult.Success)?.data)
            }

            is ListsAction.MoveItem -> _state.update { s ->
                val draft = s.reorderDraft ?: return@update s
                if (action.to !in draft.indices || action.from !in draft.indices) return@update s
                s.copy(reorderDraft = draft.toMutableList().apply { add(action.to, removeAt(action.from)) })
            }

            ListsAction.SaveReorder -> saveReorder()
            ListsAction.CancelReorder -> _state.update { it.copy(reorderDraft = null) }
            ListsAction.Shuffle -> shuffle()
            ListsAction.ShareList -> _state.value.selectedList?.let { list ->
                _events.tryEmit(ListsEvent.Share(ShareLinks.list(clubSlug, list.id), list.title))
            }
            ListsAction.ShuffleFinished -> finishShuffle()
        }
    }

    // region loading

    private fun loadClubContext() {
        viewModelScope.launch {
            memberRepository.getClubs().onSuccess { clubs ->
                clubs.firstOrNull { it.slug == clubSlug }?.let { club ->
                    _state.update { it.copy(clubType = club.type, clubName = club.name) }
                }
            }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { members ->
                _state.update { s -> s.copy(memberNames = members.associate { it.id to it.name }) }
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            if (initial) {
                _state.update { it.copy(lists = AsyncResult.Loading) }
            } else {
                _state.update { it.copy(isRefreshing = true) }
            }
            val upNextDeferred = async { listsRepository.getUpNext(clubSlug) }
            when (val result = listsRepository.getLists(clubSlug)) {
                is Result.Success -> {
                    val lists = result.data
                    val selected = _state.value.selectedListId?.takeIf { id -> lists.any { it.id == id } }
                        ?: lists.firstOrNull()?.id
                    _state.update { it.copy(lists = AsyncResult.Success(lists), selectedListId = selected) }
                    // Fetch the selected list first so it paints quickly, then the rest.
                    val ordered = lists.sortedByDescending { it.id == selected }
                    ordered.map { list -> async { loadItems(list.id, showLoading = initial) } }
                        .forEach { it.await() }
                }

                is Result.Error -> {
                    if (initial || _state.value.lists !is AsyncResult.Success) {
                        _state.update { it.copy(lists = AsyncResult.Error(dataError = result.error)) }
                    } else {
                        emitError(result.error, "refresh your lists") { load(initial = false) }
                    }
                }
            }
            upNextDeferred.await().onSuccess { id -> _state.update { it.copy(upNextWorkId = id) } }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadItems(listId: String, showLoading: Boolean) {
        if (showLoading || _state.value.itemsByList[listId] !is AsyncResult.Success) {
            _state.update { it.copy(itemsByList = it.itemsByList + (listId to AsyncResult.Loading)) }
        }
        when (val result = listsRepository.getItems(clubSlug, listId)) {
            is Result.Success -> setItems(listId, result.data)
            is Result.Error -> {
                if (_state.value.itemsByList[listId] !is AsyncResult.Success) {
                    _state.update {
                        it.copy(itemsByList = it.itemsByList + (listId to AsyncResult.Error(dataError = result.error)))
                    }
                }
            }
        }
    }

    private fun setItems(listId: String, items: List<Work>) {
        _state.update { s ->
            s.copy(
                itemsByList = s.itemsByList + (listId to AsyncResult.Success(items)),
                lists = s.lists.mapLists { lists ->
                    lists.map { if (it.id == listId) it.copy(itemCount = items.size) else it }
                },
            )
        }
    }

    private fun refreshList(listId: String) {
        viewModelScope.launch { loadItems(listId, showLoading = false) }
    }

    private fun selectList(listId: String) {
        _state.update { it.copy(selectedListId = listId, reorderDraft = null) }
        if (_state.value.itemsByList[listId] !is AsyncResult.Success) refreshList(listId)
    }

    // endregion

    // region list management

    private fun createList(title: String) {
        viewModelScope.launch {
            listsRepository.createList(clubSlug, title)
                .onSuccess { created ->
                    _state.update { s ->
                        s.copy(
                            lists = s.lists.mapLists { it + created },
                            selectedListId = created.id,
                            itemsByList = s.itemsByList + (created.id to AsyncResult.Success(emptyList())),
                        )
                    }
                    message("Created \"${created.title}\"")
                }
                .onError { emitError(it, "create the list") }
        }
    }

    private fun renameList(listId: String, title: String) {
        val before = _state.value.lists
        _state.update { s -> s.copy(lists = s.lists.mapLists { l -> l.map { if (it.id == listId) it.copy(title = title) else it } }) }
        viewModelScope.launch {
            listsRepository.renameList(clubSlug, listId, title).onError { error ->
                _state.update { it.copy(lists = before) }
                emitError(error, "rename the list")
            }
        }
    }

    private fun deleteList(listId: String) {
        val before = _state.value
        val name = before.selectedList?.takeIf { it.id == listId }?.title
            ?: (before.lists as? AsyncResult.Success)?.data?.firstOrNull { it.id == listId }?.title
        _state.update { s ->
            val remaining = (s.lists as? AsyncResult.Success)?.data.orEmpty().filter { it.id != listId }
            s.copy(
                lists = AsyncResult.Success(remaining),
                selectedListId = if (s.selectedListId == listId) remaining.firstOrNull()?.id else s.selectedListId,
            )
        }
        viewModelScope.launch {
            listsRepository.deleteList(clubSlug, listId)
                .onSuccess { message("Deleted \"${name ?: "list"}\"") }
                .onError { error ->
                    _state.update { it.copy(lists = before.lists, selectedListId = before.selectedListId) }
                    emitError(error, "delete the list")
                }
        }
    }

    private fun moveList(listId: String, offset: Int) {
        val lists = (_state.value.lists as? AsyncResult.Success)?.data ?: return
        val from = lists.indexOfFirst { it.id == listId }
        val to = from + offset
        if (from < 0 || to !in lists.indices) return
        val reordered = lists.toMutableList().apply { add(to, removeAt(from)) }
        _state.update { it.copy(lists = AsyncResult.Success(reordered)) }
        viewModelScope.launch {
            listsRepository.reorderLists(clubSlug, reordered.map { it.id }).onError { error ->
                _state.update { it.copy(lists = AsyncResult.Success(lists)) }
                emitError(error, "reorder lists")
            }
        }
    }

    // endregion

    // region items

    private fun addWork(work: NewWork) {
        val listId = _state.value.selectedListId ?: return
        val listName = _state.value.selectedList?.title ?: "the list"
        viewModelScope.launch {
            listsRepository.addItem(clubSlug, listId, work)
                .onSuccess {
                    message("Added ${work.title} to $listName")
                    loadItems(listId, showLoading = false)
                }
                .onError { error ->
                    if (error == DataError.Remote.UNKNOWN) {
                        message("Couldn't add ${work.title}. It may already be on $listName.")
                    } else {
                        emitError(error, "add ${work.title}") { addWork(work) }
                    }
                }
        }
    }

    private fun removeWork(work: Work) {
        val listId = _state.value.selectedListId ?: return
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data ?: return
        setItems(listId, before.filter { it.id != work.id })
        viewModelScope.launch {
            listsRepository.removeItem(clubSlug, listId, work.id)
                .onSuccess {
                    if (_state.value.upNextWorkId == work.id) _state.update { it.copy(upNextWorkId = null) }
                    message("Removed ${work.title}")
                }
                .onError { error ->
                    setItems(listId, before)
                    emitError(error, "remove ${work.title}")
                }
        }
    }

    private fun setUpNext(workId: String?, successText: String) {
        val previous = _state.value.upNextWorkId
        _state.update { it.copy(upNextWorkId = workId) }
        viewModelScope.launch {
            listsRepository.setUpNext(clubSlug, workId)
                .onSuccess { message(successText) }
                .onError { error ->
                    _state.update { it.copy(upNextWorkId = previous) }
                    emitError(error, "update Up Next") { setUpNext(workId, successText) }
                }
        }
    }

    private fun moveWork(work: Work, destinationListId: String) {
        val listId = _state.value.selectedListId ?: return
        if (listId == destinationListId) return
        val destName = (_state.value.lists as? AsyncResult.Success)?.data
            ?.firstOrNull { it.id == destinationListId }?.title ?: "the list"
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data ?: return
        setItems(listId, before.filter { it.id != work.id })
        viewModelScope.launch {
            listsRepository.moveItem(clubSlug, listId, work.id, destinationListId)
                .onSuccess {
                    message("Moved ${work.title} to $destName")
                    loadItems(destinationListId, showLoading = false)
                }
                .onError { error ->
                    setItems(listId, before)
                    emitError(error, "move ${work.title}")
                }
        }
    }

    private fun loadAllItems() {
        viewModelScope.launch {
            if (_state.value.allItems !is AsyncResult.Success) _state.update { it.copy(allItems = AsyncResult.Loading) }
            when (val result = listsRepository.getAllItems(clubSlug)) {
                is Result.Success -> _state.update { it.copy(allItems = AsyncResult.Success(result.data)) }
                is Result.Error -> if (_state.value.allItems !is AsyncResult.Success) {
                    _state.update { it.copy(allItems = AsyncResult.Error(dataError = result.error)) }
                }
            }
        }
    }

    /**
     * Same move the web's "queue review" does: `POST list/:from/items/:id/move` into
     * the reviews list, so the work leaves its list instead of being duplicated.
     */
    private fun moveToReviews(work: Work, fromListId: String?) {
        val listId = fromListId ?: _state.value.selectedListId ?: return
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data
        val allBefore = _state.value.allItems
        before?.let { setItems(listId, it.filter { item -> item.id != work.id }) }
        _state.update { s ->
            s.copy(allItems = (s.allItems as? AsyncResult.Success)?.let { all ->
                AsyncResult.Success(all.data.filter { it.work.id != work.id })
            } ?: s.allItems)
        }
        fun rollback() {
            before?.let { setItems(listId, it) }
            _state.update { it.copy(allItems = allBefore) }
        }
        viewModelScope.launch {
            val reviewsListId = when (val r = listsRepository.getReviewsListId(clubSlug)) {
                is Result.Success -> r.data
                is Result.Error -> {
                    rollback()
                    emitError(r.error, "move ${work.title} to reviews")
                    return@launch
                }
            }
            listsRepository.moveItem(clubSlug, listId, work.id, reviewsListId)
                .onSuccess {
                    if (_state.value.upNextWorkId == work.id) _state.update { it.copy(upNextWorkId = null) }
                    if (before == null) {
                        // Not loaded locally; keep the list's item count honest.
                        _state.update { s ->
                            s.copy(lists = s.lists.mapLists { lists ->
                                lists.map { if (it.id == listId) it.copy(itemCount = (it.itemCount - 1).coerceAtLeast(0)) else it }
                            })
                        }
                    }
                    _events.emit(ListsEvent.MovedToReviews(work.title))
                }
                .onError { error ->
                    rollback()
                    emitError(error, "move ${work.title} to reviews")
                }
        }
    }

    private fun saveReorder() {
        val listId = _state.value.selectedListId ?: return
        val draft = _state.value.reorderDraft ?: return
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data ?: return
        _state.update { it.copy(reorderDraft = null) }
        if (draft.map { it.id } == before.map { it.id }) return
        setItems(listId, draft)
        viewModelScope.launch {
            listsRepository.reorderItems(clubSlug, listId, draft.map { it.id })
                .onSuccess { message("Order saved") }
                .onError { error ->
                    setItems(listId, before)
                    emitError(error, "save the new order")
                }
        }
    }

    // endregion

    // region shuffle

    private fun shuffle() {
        val items = (_state.value.selectedItems as? AsyncResult.Success)?.data.orEmpty()
        val candidates = items.filter { it.id != _state.value.upNextWorkId }.ifEmpty { items }
        if (candidates.isEmpty()) {
            message("Add something to this list first, then shuffle.")
            return
        }
        _state.update { it.copy(shuffle = ShuffleState(candidates = items, selected = candidates.random())) }
    }

    private fun finishShuffle() {
        val picked = _state.value.shuffle?.selected ?: return
        _state.update { it.copy(shuffle = null) }
        setUpNext(picked.id, "The pick is ${picked.title}! It's now up next.")
    }

    // endregion

    private fun message(text: String) {
        _events.tryEmit(ListsEvent.Message(UiMessage(text)))
    }

    private fun emitError(error: DataError.Remote, action: String, retry: (() -> Unit)? = null) {
        val canRetry = retry != null && error.isRetryable
        _events.tryEmit(
            ListsEvent.Message(
                UiMessage(
                    text = error.toUserMessage(action),
                    actionLabel = if (canRetry) "Retry" else null,
                    onAction = if (canRetry) retry else null,
                )
            )
        )
    }
}

private fun AsyncResult<List<WorkList>>.mapLists(transform: (List<WorkList>) -> List<WorkList>): AsyncResult<List<WorkList>> =
    if (this is AsyncResult.Success) AsyncResult.Success(transform(data)) else this
