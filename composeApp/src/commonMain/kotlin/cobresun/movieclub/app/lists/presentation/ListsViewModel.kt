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
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
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
    val itemsByList: Map<String, AsyncResult<List<Work>>> = emptyMap(),
    /** Lists the user folded away (persisted per club, like web). */
    val collapsedListIds: Set<String> = emptySet(),
    val upNextWorkId: String? = null,
    /** Filters every list at once, like web's search bar above the lists. */
    val filter: String = "",
    val isRefreshing: Boolean = false,
    /** Club members by id, for "Added by". */
    val members: Map<String, Member> = emptyMap(),
    /** Non-null while the random picker runs. */
    val shuffle: ShuffleState? = null,
    /** Non-null while reordering one list. */
    val reorderDraft: ReorderDraft? = null,
    /** Every item across the user lists, for picking a review in the Add review sheet. */
    val allItems: AsyncResult<List<ListedWork>> = AsyncResult.Loading,
) {
    /** [allItems] with the Up Next work first, then list order. */
    val pickableItems: List<ListedWork>
        get() = (allItems as? AsyncResult.Success)?.data.orEmpty()
            .sortedByDescending { it.work.id == upNextWorkId }

    val listsOrEmpty: List<WorkList>
        get() = (lists as? AsyncResult.Success)?.data.orEmpty()

    fun list(listId: String?): WorkList? = listsOrEmpty.firstOrNull { it.id == listId }

    fun items(listId: String): AsyncResult<List<Work>> = itemsByList[listId] ?: AsyncResult.Loading

    fun itemsOrEmpty(listId: String): List<Work> = (itemsByList[listId] as? AsyncResult.Success)?.data.orEmpty()

    /** A list's items that match [filter], in list order. */
    fun visibleItems(listId: String): List<Work> = itemsOrEmpty(listId).filter { it.matchesQuery(filter) }

    fun isCollapsed(listId: String): Boolean = listId in collapsedListIds
}

data class ReorderDraft(val listId: String, val items: List<Work>)

data class ShuffleState(val listId: String, val candidates: List<Work>, val selected: Work)

sealed interface ListsEvent {
    data class Message(val message: UiMessage) : ListsEvent

    /** A work left the lists for the reviews tab. */
    data class MovedToReviews(val title: String) : ListsEvent

    /** Open the system share sheet for a public link. */
    data class Share(val url: String, val title: String) : ListsEvent
}

sealed interface ListsAction {
    data object Refresh : ListsAction
    data class Filter(val query: String) : ListsAction
    data class ToggleCollapsed(val listId: String) : ListsAction

    data class CreateList(val title: String) : ListsAction
    data class RenameList(val listId: String, val title: String) : ListsAction
    data class DeleteList(val listId: String) : ListsAction
    data class MoveList(val listId: String, val offset: Int) : ListsAction

    data class AddWork(val work: NewWork, val listId: String) : ListsAction
    data class RemoveWork(val work: Work, val listId: String) : ListsAction
    data class SetUpNext(val work: Work) : ListsAction
    data object ClearUpNext : ListsAction
    data class MoveWork(val work: Work, val fromListId: String, val destinationListId: String) : ListsAction
    /** Moves [work] from [fromListId] into reviews. */
    data class MoveToReviews(val work: Work, val fromListId: String) : ListsAction

    /** (Re)loads [ListsState.allItems] for the Add review sheet. */
    data object LoadAllItems : ListsAction

    data class StartReorder(val listId: String) : ListsAction
    data class MoveItem(val from: Int, val to: Int) : ListsAction
    data object SaveReorder : ListsAction
    data object CancelReorder : ListsAction

    data class Shuffle(val listId: String) : ListsAction
    data class ShareList(val listId: String) : ListsAction
    data object ShuffleFinished : ListsAction
}

class ListsViewModel(
    savedStateHandle: SavedStateHandle,
    private val listsRepository: ListsRepository,
    private val memberRepository: MemberRepository,
    private val clubRepository: ClubRepository,
    private val collapsedListsStore: CollapsedListsStore,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    private val _state = MutableStateFlow(ListsState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ListsEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val collapsed = collapsedListsStore.get(clubSlug)
            _state.update { it.copy(collapsedListIds = collapsed) }
        }
        loadClubContext()
        load(initial = true)
    }

    fun onAction(action: ListsAction) {
        when (action) {
            ListsAction.Refresh -> {
                if (_state.value.clubName.isBlank()) loadClubContext()
                load(initial = false)
            }
            is ListsAction.Filter -> _state.update { it.copy(filter = action.query) }
            is ListsAction.ToggleCollapsed -> toggleCollapsed(action.listId)
            is ListsAction.CreateList -> createList(action.title)
            is ListsAction.RenameList -> renameList(action.listId, action.title)
            is ListsAction.DeleteList -> deleteList(action.listId)
            is ListsAction.MoveList -> moveList(action.listId, action.offset)
            is ListsAction.AddWork -> addWork(action.work, action.listId)
            is ListsAction.RemoveWork -> runItemOp(ListItemOp.Remove(action.work, action.listId))
            is ListsAction.SetUpNext -> setUpNext(action.work.id, "${action.work.title} is up next")
            ListsAction.ClearUpNext -> setUpNext(null, "Up Next cleared")
            is ListsAction.MoveWork -> if (action.fromListId != action.destinationListId) {
                runItemOp(ListItemOp.Move(action.work, action.fromListId, action.destinationListId))
            }
            is ListsAction.MoveToReviews -> moveToReviews(action.work, action.fromListId)
            ListsAction.LoadAllItems -> loadAllItems()
            is ListsAction.StartReorder -> _state.update {
                it.copy(reorderDraft = ReorderDraft(action.listId, it.itemsOrEmpty(action.listId)))
            }

            is ListsAction.MoveItem -> _state.update { s ->
                val draft = s.reorderDraft ?: return@update s
                val items = draft.items
                if (action.to !in items.indices || action.from !in items.indices) return@update s
                s.copy(reorderDraft = draft.copy(items = items.toMutableList().apply { add(action.to, removeAt(action.from)) }))
            }

            ListsAction.SaveReorder -> saveReorder()
            ListsAction.CancelReorder -> _state.update { it.copy(reorderDraft = null) }
            is ListsAction.Shuffle -> shuffle(action.listId)
            is ListsAction.ShareList -> _state.value.list(action.listId)?.let { list ->
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
                _state.update { s -> s.copy(members = members.associateBy { it.id }) }
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
                    _state.update { it.copy(lists = AsyncResult.Success(lists)) }
                    // Expanded lists first so what's on screen paints quickly, then the rest.
                    val ordered = lists.sortedBy { _state.value.isCollapsed(it.id) }
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

    private fun toggleCollapsed(listId: String) {
        val next = _state.value.collapsedListIds.let { if (listId in it) it - listId else it + listId }
        _state.update { it.copy(collapsedListIds = next) }
        viewModelScope.launch { collapsedListsStore.set(clubSlug, next) }
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
        val name = before.list(listId)?.title
        _state.update { s -> s.copy(lists = AsyncResult.Success(s.listsOrEmpty.filter { it.id != listId })) }
        viewModelScope.launch {
            listsRepository.deleteList(clubSlug, listId)
                .onSuccess { message("Deleted \"${name ?: "list"}\"") }
                .onError { error ->
                    _state.update { it.copy(lists = before.lists) }
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

    private fun addWork(work: NewWork, listId: String) {
        val listName = _state.value.list(listId)?.title ?: "the list"
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
                        emitError(error, "add ${work.title}") { addWork(work, listId) }
                    }
                }
        }
    }

    /**
     * Removes or moves an item out of [ListItemOp.sourceListId], optimistically.
     * The op carries every id it needs, so Retry replays exactly the failed
     * operation even if the user has since switched lists.
     */
    private fun runItemOp(op: ListItemOp) {
        val work = op.work
        val listId = op.sourceListId
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data
        before?.let { setItems(listId, it.filter { item -> item.id != work.id }) }
        viewModelScope.launch {
            val result = when (op) {
                is ListItemOp.Remove -> listsRepository.removeItem(clubSlug, listId, work.id)
                is ListItemOp.Move -> listsRepository.moveItem(clubSlug, listId, work.id, op.destinationListId)
            }
            result
                .onSuccess {
                    when (op) {
                        is ListItemOp.Remove -> {
                            if (_state.value.upNextWorkId == work.id) _state.update { it.copy(upNextWorkId = null) }
                            message("Removed ${work.title}")
                        }
                        is ListItemOp.Move -> {
                            val destName = (_state.value.lists as? AsyncResult.Success)?.data
                                ?.firstOrNull { it.id == op.destinationListId }?.title ?: "the list"
                            message("Moved ${work.title} to $destName")
                            loadItems(op.destinationListId, showLoading = false)
                        }
                    }
                }
                .onError { error ->
                    // Roll back only what we changed, and only if nothing reloaded the list since.
                    if (before != null) {
                        val now = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data
                        if (now != null && now.none { it.id == work.id }) setItems(listId, before)
                    }
                    val verb = if (op is ListItemOp.Move) "move" else "remove"
                    emitError(error, "$verb ${work.title}") { runItemOp(op) }
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
    private fun moveToReviews(work: Work, listId: String) {
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
                    emitError(r.error, "move ${work.title} to reviews") { moveToReviews(work, listId) }
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
                    emitError(error, "move ${work.title} to reviews") { moveToReviews(work, listId) }
                }
        }
    }

    private fun saveReorder() {
        val listId = _state.value.reorderDraft?.listId ?: return
        val draft = _state.value.reorderDraft?.items ?: return
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

    private fun shuffle(listId: String) {
        val items = _state.value.visibleItems(listId)
        val candidates = items.filter { it.id != _state.value.upNextWorkId }.ifEmpty { items }
        if (candidates.isEmpty()) {
            message("Add something to this list first, then shuffle.")
            return
        }
        _state.update { it.copy(shuffle = ShuffleState(listId, candidates = items, selected = candidates.random())) }
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

/** A list-item change captured with all its ids, so it can be replayed verbatim on Retry. */
sealed interface ListItemOp {
    val work: Work
    val sourceListId: String

    data class Remove(override val work: Work, override val sourceListId: String) : ListItemOp
    data class Move(override val work: Work, override val sourceListId: String, val destinationListId: String) : ListItemOp
}
