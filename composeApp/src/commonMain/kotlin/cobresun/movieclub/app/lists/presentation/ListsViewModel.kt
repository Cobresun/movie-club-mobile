package cobresun.movieclub.app.lists.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.mapSuccess
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.toAsyncResult
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.toUiMessage
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
import cobresun.movieclub.app.lists.domain.withSubsetReordered
import cobresun.movieclub.app.lists.domain.withFirst
import cobresun.movieclub.app.core.domain.WorkDetailsRepository
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import kotlin.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import cobresun.movieclub.app.core.domain.ShareLinks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import cobresun.movieclub.app.reviews.presentation.CommentThread



class ListsViewModel(
    private val clubSlug: String,
    private val listsRepository: ListsRepository,
    private val memberRepository: MemberRepository,
    private val clubRepository: ClubRepository,
    private val collapsedListsStore: CollapsedListsStore,
    private val workDetailsRepository: WorkDetailsRepository,
    private val commentsRepository: ReviewCommentsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ListsState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ListsEvent>(extraBufferCapacity = 8)

    private val comments = CommentThread(
        scope = viewModelScope,
        repository = commentsRepository,
        clubSlug = clubSlug,
        open = { _state.value.detail?.let { it.workId to it.thread } },
        me = { _state.value.currentUserId?.let { _state.value.members[it] } },
        update = { workId, transform -> updateDetail(workId) { it.copy(thread = transform(it.thread)) } },
        onError = { error, action -> emitError(error, action) },
    )
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
            is ListsAction.Filter -> _state.update { it.copy(filterQuery = action.query) }
            is ListsAction.ToggleCollapsed -> toggleCollapsed(action.listId)
            is ListsAction.CreateList -> createList(action.title)
            is ListsAction.RenameList -> renameList(action.listId, action.title)
            is ListsAction.DeleteList -> deleteList(action.listId)
            is ListsAction.MoveList -> moveList(action.listId, action.offset)
            is ListsAction.AddWork -> addWork(action.work, action.listId)
            is ListsAction.RemoveWork -> runItemOp(ListItemOp.Remove(action.work, action.listId))
            is ListsAction.SetUpNext -> {
                setUpNext(action.work.id, "${action.work.title} is up next")
                moveToFront(action.work.id)
            }
            ListsAction.ClearUpNext -> setUpNext(null, "Up Next cleared")
            is ListsAction.MoveWork -> if (action.fromListId != action.destinationListId) {
                runItemOp(ListItemOp.Move(action.work, action.fromListId, action.destinationListId))
            }
            is ListsAction.MoveToReviews -> moveToReviews(action.work, action.fromListId)
            ListsAction.LoadAllItems -> loadAllItems()
            is ListsAction.Reorder -> reorder(action.listId, action.workIds, action.droppedId)
            is ListsAction.Shuffle -> shuffle(action.listId)
            is ListsAction.ShareList -> _state.value.list(action.listId)?.let { list ->
                _events.tryEmit(ListsEvent.Share(ShareLinks.list(clubSlug, list.id), list.title))
            }
            ListsAction.CloseShuffle -> _state.update { it.copy(shuffle = null) }
            ListsAction.PulseShown -> _state.update { it.copy(pulse = null) }
            is ListsAction.OpenDetail -> {
                _state.update { it.copy(detail = ListItemDetailState(action.listId, action.workId)) }
                loadDetail(action.workId)
            }
            ListsAction.CloseDetail -> _state.update { it.copy(detail = null) }
            is ListsAction.AddComment -> comments.add(action.content, action.spoiler)
            is ListsAction.EditComment -> comments.edit(action.commentId, action.content, action.spoiler)
            is ListsAction.DeleteComment -> comments.delete(action.commentId)
            ListsAction.RetryComments -> comments.retry()
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
            memberRepository.getMember().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { members ->
                _state.update { s -> s.copy(members = members.associateBy { it.id }) }
            }
        }
    }

    /** The connection is back: reload what's on screen without a refresh spinner. */
    fun reloadQuietly() {
        if (_state.value.clubName.isBlank()) loadClubContext()
        load(initial = _state.value.lists !is AsyncResult.Success, showIndicator = false)
        _state.value.detail?.let { loadDetail(it.workId) }
    }

    private fun load(initial: Boolean, showIndicator: Boolean = true) {
        viewModelScope.launch {
            if (initial) {
                // Paint the lists kept from last time at once, then revalidate below.
                val saved = listsRepository.savedLists(clubSlug)
                val savedItems = saved?.associate { list ->
                    list.id to (listsRepository.savedItems(clubSlug, list.id)?.let { AsyncResult.Success(it) } ?: AsyncResult.Loading)
                }
                _state.update {
                    if (saved != null) it.copy(lists = AsyncResult.Success(saved), itemsByList = savedItems.orEmpty())
                    else it.copy(lists = AsyncResult.Loading)
                }
            } else if (showIndicator) {
                _state.update { it.copy(isRefreshing = true) }
            }
            val upNextDeferred = async { listsRepository.getUpNext(clubSlug, refresh = !initial) }
            when (val result = listsRepository.getLists(clubSlug)) {
                is Result.Success -> {
                    val lists = result.data
                    _state.update { s ->
                        s.copy(
                            lists = AsyncResult.Success(lists),
                            // Kept items stay on screen until their list's fresh copy lands.
                            itemsByList = if (initial) {
                                lists.associate { list -> list.id to (s.itemsByList[list.id] as? AsyncResult.Success ?: AsyncResult.Loading) }
                            } else {
                                s.itemsByList
                            },
                        )
                    }
                    // Fetch every list at once and publish them together: one state update
                    // (one recomposition and filter pass) instead of two per list.
                    val fetched = lists.map { list -> async { list.id to listsRepository.getItems(clubSlug, list.id) } }
                        .map { it.await() }
                    _state.update { s ->
                        var byList = s.itemsByList
                        var counts = s.lists
                        fetched.forEach { (listId, items) ->
                            when (items) {
                                is Result.Success -> {
                                    byList = byList + (listId to AsyncResult.Success(items.data))
                                    counts = counts.mapSuccess { all -> all.map { if (it.id == listId) it.copy(itemCount = items.data.size) else it } }
                                }
                                is Result.Error -> if (byList[listId] !is AsyncResult.Success) {
                                    byList = byList + (listId to AsyncResult.Error(dataError = items.error))
                                }
                            }
                        }
                        s.copy(itemsByList = byList, lists = counts)
                    }
                }

                is Result.Error -> {
                    if (_state.value.lists !is AsyncResult.Success) {
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
                lists = s.lists.mapSuccess { lists ->
                    lists.map { if (it.id == listId) it.copy(itemCount = items.size) else it }
                },
            )
        }
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
                            lists = s.lists.mapSuccess { it + created },
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
        _state.update { s -> s.copy(lists = s.lists.mapSuccess { l -> l.map { if (it.id == listId) it.copy(title = title) else it } }) }
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

    /**
     * Like web, the work shows up at the end of the list right away as a
     * pending card (not openable or draggable) until the add settles and the
     * list reloads with its real id.
     */
    private fun addWork(work: NewWork, listId: String) {
        val pending = Work(
            id = "$PENDING_PREFIX${++pendingCount}",
            type = work.type,
            title = work.title,
            addedAt = Clock.System.now().toString(),
            addedDate = null,
            imageUrl = work.imageUrl,
            externalId = work.externalId,
            addedByUserId = _state.value.currentUserId,
            details = null,
        )
        (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data?.let { setItems(listId, it + pending) }
        viewModelScope.launch {
            listsRepository.addItem(clubSlug, listId, work)
                .onSuccess { message("Added \"${work.title}\" to the list") }
                .onError { error -> emitError(error, "add \"${work.title}\"") { addWork(work, listId) } }
            // Settled either way: reload so the pending card becomes the real one (or goes).
            when (val reloaded = listsRepository.getItems(clubSlug, listId)) {
                is Result.Success -> setItems(listId, reloaded.data)
                is Result.Error -> _state.value.itemsOrEmpty(listId).let { items ->
                    setItems(listId, items.filter { it.id != pending.id })
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
        if (workId != null) pulse(workId)
        viewModelScope.launch {
            listsRepository.setUpNext(clubSlug, workId)
                .onSuccess { message(successText) }
                .onError { error ->
                    _state.update { it.copy(upNextWorkId = previous) }
                    emitError(error, "update Up Next") { setUpNext(workId, successText) }
                }
        }
    }

    /** Like web, a new Up Next also moves to the front of its list, and that order is saved. */
    private fun moveToFront(workId: String) {
        val listId = _state.value.itemsByList.entries.firstOrNull { (_, items) ->
            (items as? AsyncResult.Success)?.data?.any { it.id == workId } == true
        }?.key ?: return
        val before = _state.value.itemsOrEmpty(listId)
        val after = before.withFirst(workId) { it.id }
        if (after.map { it.id } == before.map { it.id }) return
        setItems(listId, after)
        viewModelScope.launch {
            listsRepository.reorderItems(clubSlug, listId, after.map { it.id }).onError { error ->
                if (_state.value.itemsOrEmpty(listId).map { it.id } == after.map { it.id }) setItems(listId, before)
                emitError(error, "move it to the front of the list") { moveToFront(workId) }
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
                            s.copy(lists = s.lists.mapSuccess { lists ->
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

    private fun reorder(listId: String, workIds: List<String>, droppedId: String) {
        val before = (_state.value.itemsByList[listId] as? AsyncResult.Success)?.data ?: return
        val byId = before.associateBy { it.id }
        val moved = workIds.mapNotNull { byId[it] }
        val after = before.withSubsetReordered(moved) { it.id }
        if (after.map { it.id } == before.map { it.id }) return
        setItems(listId, after)
        pulse(droppedId)
        viewModelScope.launch {
            listsRepository.reorderItems(clubSlug, listId, moved.map { it.id }).onError { error ->
                setItems(listId, before)
                emitError(error, "save the new order") { reorder(listId, workIds, droppedId) }
            }
        }
    }

    // endregion

    // region details sheet

    private fun loadDetail(workId: String) {
        viewModelScope.launch {
            val result = workDetailsRepository.getWorkDetails(clubSlug, workId)
            updateDetail(workId) {
                it.copy(
                    details = result.toAsyncResult()
                )
            }
        }
        comments.load(workId)
    }

    private fun updateDetail(workId: String, transform: (ListItemDetailState) -> ListItemDetailState) {
        _state.update { s -> if (s.detail?.workId == workId) s.copy(detail = transform(s.detail)) else s }
    }

    // endregion

    // region shuffle

    private fun shuffle(listId: String) {
        // Like web, any shown item can win, Up Next included.
        val items = _state.value.visibleItems(listId).filterNot { it.isPending }
        if (items.isEmpty()) {
            message("Add something to this list first, then shuffle.")
            return
        }
        _state.update { it.copy(shuffle = ShuffleState(listId, candidates = items, selected = items.random())) }
    }

    // endregion

    private var pendingCount = 0

    private var pulseCount = 0

    private fun pulse(workId: String) {
        _state.update { it.copy(pulse = CardPulse(workId, ++pulseCount)) }
    }

    private fun message(text: String) {
        _events.tryEmit(ListsEvent.Message(UiMessage(text)))
    }

    private fun emitError(error: DataError.Remote, action: String, retry: (() -> Unit)? = null) {
        _events.tryEmit(ListsEvent.Message(error.toUiMessage(action, retry)))
    }
}
