package cobresun.movieclub.app.reviews.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.toAsyncResult
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetailsRepository
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.domain.ShareLinks
import cobresun.movieclub.app.filters.domain.FilterQuery
import kotlinx.datetime.LocalDate
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.toUiMessage
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.domain.ScoreSave
import cobresun.movieclub.app.reviews.domain.ScoreScale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



class ReviewsViewModel(
    private val clubSlug: String,
    private val savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val commentsRepository: ReviewCommentsRepository,
    private val memberRepository: MemberRepository,
    private val clubRepository: ClubRepository,
    private val listsRepository: ListsRepository,
    private val workDetailsRepository: WorkDetailsRepository,
) : ViewModel() {

    // The search text and open details sheet survive process death like the selected tab does.
    private val _state = MutableStateFlow(
        ReviewsState(
            clubSlug = clubSlug,
            filterQuery = FilterQuery(text = savedStateHandle.get<String>(KEY_QUERY).orEmpty()),
            detail = savedStateHandle.get<String>(KEY_DETAIL)?.let { ReviewDetailState(it) },
        )
    )
    val state = _state.asStateFlow()

    private val scorePolls = mutableMapOf<String, Job>()

    private val _events = MutableSharedFlow<ReviewsEvent>(extraBufferCapacity = 8)

    private val comments = CommentThread(
        scope = viewModelScope,
        repository = commentsRepository,
        clubSlug = clubSlug,
        open = { _state.value.detail?.let { it.workId to it.thread } },
        me = { _state.value.members.firstOrNull { it.id == _state.value.currentUserId } },
        update = { workId, transform -> updateDetail(workId) { it.copy(thread = transform(it.thread)) } },
        onError = { error, action -> emitError(error, action) },
    )
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            clubRepository.observeFeatures(clubSlug)
                .collect { features -> _state.update { it.copy(discussionQuestionsEnabled = features.discussionQuestions) } }
        }
        loadContext()
        loadReviews(initial = true)
        _state.value.detail?.let { loadDetail(it.workId) }
    }

    fun onAction(action: ReviewsAction) {
        when (action) {
            ReviewsAction.Shown -> loadUpNext()
            ReviewsAction.StartUpNextReview -> startUpNextReview()
            ReviewsAction.Refresh -> {
                if (_state.value.clubName.isBlank()) loadContext()
                loadReviews(initial = false)
                loadUpNext(refresh = true)
                _state.value.detail?.let { loadDetail(it.workId) }
            }

            is ReviewsAction.Filter -> {
                savedStateHandle[KEY_QUERY] = action.query.text
                _state.update { it.copy(filterQuery = action.query) }
            }
            is ReviewsAction.Sort -> _state.update { it.copy(sortState = ReviewSortState(action.option, descending = true)) }

            ReviewsAction.ToggleSortDirection -> _state.update {
                it.copy(sortState = it.sortState.copy(descending = !it.sortState.descending))
            }

            is ReviewsAction.AddReview -> addReview(action.work)
            is ReviewsAction.ShowPending -> showPending(action.work)
            is ReviewsAction.RemoveReview -> removeReview(action.work)
            is ReviewsAction.Share -> share(action.work)
            ReviewsAction.RetryComments -> comments.retry()

            is ReviewsAction.OpenDetail -> {
                savedStateHandle[KEY_DETAIL] = action.workId
                _state.update { it.copy(detail = ReviewDetailState(action.workId)) }
                loadDetail(action.workId)
            }

            ReviewsAction.CloseDetail -> {
                savedStateHandle.remove<String>(KEY_DETAIL)
                _state.update { it.copy(detail = null) }
            }
            is ReviewsAction.SaveScore -> saveScore(action.workId, action.score)
            is ReviewsAction.SetReviewedDate -> setReviewedDate(action.workId, action.date)
            is ReviewsAction.DeleteScore -> deleteScore(action.workId)
            is ReviewsAction.RevealScores -> _state.update { it.copy(revealedWorkIds = it.revealedWorkIds + action.workId) }
            is ReviewsAction.AddComment -> comments.add(action.content, action.spoiler)
            is ReviewsAction.EditComment -> comments.edit(action.commentId, action.content, action.spoiler)
            is ReviewsAction.DeleteComment -> comments.delete(action.commentId)
            ReviewsAction.GenerateQuestions -> generateQuestions()
        }
    }

    /** The connection is back: reload what's on screen without a refresh spinner. */
    fun reloadQuietly() {
        if (_state.value.clubName.isBlank()) loadContext()
        viewModelScope.launch { fetchReviews() }
        loadUpNext()
        _state.value.detail?.let { loadDetail(it.workId) }
    }

    /** Called when a work arrives from the lists tab. */
    fun refreshQuietly() {
        loadReviews(initial = false)
        loadUpNext(refresh = true)
    }

    /**
     * Up Next and the club's listed works. On each show this is the shared copy unless
     * it's over a minute old or a list write since renewed it, so tab switches are free.
     */
    private fun loadUpNext(refresh: Boolean = false) {
        viewModelScope.launch {
            listsRepository.getUpNext(clubSlug, refresh).onSuccess { id -> _state.update { it.copy(upNextWorkId = id) } }
        }
        viewModelScope.launch {
            listsRepository.getAllItems(clubSlug, refresh).onSuccess { items -> _state.update { it.copy(listedWorks = items) } }
        }
    }

    /**
     * Web UpNextCard "Review": moves the pinned work from its list into reviews
     * (so it leaves the list rather than being duplicated), then opens its detail
     * sheet, where score entry lives.
     */
    private fun startUpNextReview() {
        val item = _state.value.upNext ?: return
        if (_state.value.startingUpNext != null) return
        _state.update { it.copy(startingUpNext = item) }
        showPending(item.work)
        viewModelScope.launch {
            val moved = when (val listId = listsRepository.getReviewsListId(clubSlug)) {
                is Result.Success -> listsRepository.moveItem(clubSlug, item.listId, item.work.id, listId.data)
                is Result.Error -> listId
            }
            when (moved) {
                is Result.Success -> {
                    _state.update { s -> s.copy(listedWorks = s.listedWorks.filter { it.work.id != item.work.id }) }
                    fetchReviews()
                    if (_state.value.review(item.work.id) != null) onAction(ReviewsAction.OpenDetail(item.work.id))
                }
                is Result.Error -> {
                    clearPending("$PENDING_WORK_PREFIX${item.work.externalId ?: item.work.title}")
                    message("Failed to add \"${item.work.title}\" to reviews. Please try again.")
                }
            }
            _state.update { it.copy(startingUpNext = null) }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            memberRepository.getClubs().onSuccess { clubs ->
                clubs.firstOrNull { it.slug == clubSlug }?.let { club -> _state.update { it.copy(clubType = club.type, clubName = club.name) } }
            }
        }
        viewModelScope.launch {
            memberRepository.getMember().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { members -> _state.update { it.copy(members = members) } }
        }
        loadMyScoredWorks()
        // Lands in observeFeatures (init), which also follows Club settings' toggles.
        viewModelScope.launch { clubRepository.getFeatures(clubSlug) }
    }

    private fun loadMyScoredWorks() {
        viewModelScope.launch {
            reviewsRepository.getMyScoredWorks().onSuccess { works -> _state.update { it.copy(myScoredWorks = works) } }
        }
    }

    private fun loadReviews(initial: Boolean) {
        viewModelScope.launch {
            if (initial) {
                // Paint the reviews kept from last time at once, then revalidate below.
                val saved = reviewsRepository.savedReviews(clubSlug)
                _state.update { it.copy(reviews = if (saved != null) AsyncResult.Success(saved) else AsyncResult.Loading) }
            } else {
                _state.update { it.copy(isRefreshing = true) }
            }
            // The first load shares the copy the club's other tabs are fetching.
            fetchReviews(refresh = !initial)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun fetchReviews(refresh: Boolean = true) {
        when (val result = reviewsRepository.getReviews(clubSlug, refresh)) {
            is Result.Success -> _state.update { s ->
                val landed = result.data.mapNotNull { it.work.externalId }.toSet()
                s.copy(
                    reviews = AsyncResult.Success(result.data),
                    pendingWorks = s.pendingWorks.filter { it.externalId !in landed },
                )
            }
            is Result.Error -> if (_state.value.reviews is AsyncResult.Success) {
                emitError(result.error, "refresh reviews") { loadReviews(initial = false) }
            } else {
                _state.update { it.copy(reviews = AsyncResult.Error(dataError = result.error)) }
            }
        }
    }

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

    private fun updateDetail(workId: String, transform: (ReviewDetailState) -> ReviewDetailState) {
        _state.update { s -> if (s.detail?.workId == workId) s.copy(detail = transform(s.detail)) else s }
    }

    private fun updateReview(workId: String, transform: (Review) -> Review) {
        _state.update { s ->
            val list = (s.reviews as? AsyncResult.Success)?.data ?: return@update s
            var changed = false
            val next = list.map { review ->
                if (review.work.id != workId) review else transform(review).also { if (it != review) changed = true }
            }
            // An unchanged review (e.g. a score poll tick with nothing new) emits nothing,
            // so the grid, filters and open sheet aren't recomputed for it.
            if (changed) s.copy(reviews = AsyncResult.Success(next)) else s
        }
    }

    /**
     * Puts [work] at the top of the grid as a pending card until it shows up in
     * a refetch (web `addPendingReview`). Expires on its own in case the write
     * that adds it fails somewhere we don't hear about.
     */
    private fun showPending(work: Work) {
        val pending = work.copy(id = "$PENDING_WORK_PREFIX${work.externalId ?: work.title}")
        _state.update { s -> s.copy(pendingWorks = listOf(pending) + s.pendingWorks.filter { it.id != pending.id }) }
        viewModelScope.launch {
            delay(PENDING_EXPIRY_MS)
            clearPending(pending.id)
        }
    }

    private fun clearPending(id: String) {
        _state.update { s -> s.copy(pendingWorks = s.pendingWorks.filter { it.id != id }) }
    }

    private fun addReview(work: NewWork) {
        val pendingId = "$PENDING_WORK_PREFIX${work.externalId}"
        showPending(
            Work(
                id = "", type = work.type, title = work.title, addedAt = "", addedDate = null,
                imageUrl = work.imageUrl, externalId = work.externalId, addedByUserId = null, details = null,
            )
        )
        viewModelScope.launch {
            reviewsRepository.addReview(clubSlug, work)
                .onSuccess {
                    message("Added ${work.title} to reviews")
                    loadReviews(initial = false)
                }
                .onError { error ->
                    clearPending(pendingId)
                    if (error == DataError.Remote.UNKNOWN) message("Couldn't add ${work.title}. It may already be reviewed.")
                    else emitError(error, "add ${work.title}") { addReview(work) }
                }
        }
    }

    private fun removeReview(work: Work) {
        val before = _state.value.reviews
        _state.update { s ->
            s.copy(
                reviews = (s.reviews as? AsyncResult.Success)?.let { r -> AsyncResult.Success(r.data.filter { it.work.id != work.id }) } ?: s.reviews,
                detail = if (s.detail?.workId == work.id) null else s.detail,
            )
        }
        viewModelScope.launch {
            reviewsRepository.removeReview(clubSlug, work.id)
                .onSuccess { message("Removed ${work.title} from reviews") }
                .onError { error ->
                    _state.update { it.copy(reviews = before) }
                    emitError(error, "remove ${work.title}")
                }
        }
    }

    private fun share(work: Work) {
        _events.tryEmit(ReviewsEvent.Share(ShareLinks.review(clubSlug, work.id), work.title))
    }

    private fun saveScore(workId: String, score: Double) {
        val userId = _state.value.currentUserId ?: run {
            message("Still loading your profile. Try again in a moment.")
            return
        }
        if (!ScoreScale.isValid(score)) {
            message("Scores go from 0 to 10.")
            return
        }
        val review = _state.value.review(workId) ?: return
        val existing = review.scoreOf(userId)
        updateReview(workId) { it.withScore(userId, MemberScore(existing?.id ?: MemberScore.PENDING_ID, userId, score)) }
        updateDetail(workId) { it.copy(isSavingScore = true) }
        viewModelScope.launch {
            reviewsRepository.saveScore(clubSlug, workId, userId, existing?.id, score)
                .onSuccess { saved ->
                    if (saved == ScoreSave.QUEUED) {
                        message("You're offline. Your ${ScoreScale.format(score)} will be saved when you reconnect.")
                        return@onSuccess
                    }
                    message("Score saved: ${ScoreScale.format(score)}")
                    loadMyScoredWorks()
                    // The poll's first tick brings back the server's score id and average,
                    // so there's no need to refetch (and re-map) every review.
                    startScorePoll(workId)
                }
                .onError { error ->
                    updateReview(workId) { review }
                    emitError(error, "save your score") { saveScore(workId, score) }
                    fetchReviews()
                }
            updateDetail(workId) { it.copy(isSavingScore = false) }
        }
    }

    /**
     * After you score, briefly poll the work's scores so everyone else's (scored
     * around the same time in a synchronized session) stream in without a manual
     * refresh. Mirrors web `startScorePoll`: up to 10 tries 500ms apart, stopping
     * early once every member has scored; a new save on the work restarts it.
     */
    private fun startScorePoll(workId: String) {
        scorePolls.remove(workId)?.cancel()
        scorePolls[workId] = viewModelScope.launch {
            repeat(SCORE_POLL_ATTEMPTS) {
                delay(SCORE_POLL_INTERVAL_MS)
                val result = reviewsRepository.getWorkScores(clubSlug, workId)
                if (result is Result.Success) {
                    updateReview(workId) { it.withScores(result.data) }
                    val members = _state.value.members
                    if (members.isNotEmpty() && members.all { it.id in result.data.scores }) return@launch
                }
                // A failed tick is transient; a later one may succeed.
            }
        }.also { job -> job.invokeOnCompletion { if (scorePolls[workId] === job) scorePolls.remove(workId) } }
    }

    private fun setReviewedDate(workId: String, date: LocalDate) {
        val review = _state.value.review(workId) ?: return
        if (review.work.addedDate == date) return
        updateReview(workId) { it.copy(work = it.work.copy(addedDate = date)) }
        viewModelScope.launch {
            reviewsRepository.updateReviewedDate(clubSlug, workId, date)
                .onSuccess { message("Reviewed date updated") }
                .onError { error ->
                    updateReview(workId) { review }
                    emitError(error, "change the date")
                }
            // Quietly (no refresh spinner): the server's timestamp re-sorts the grid.
            fetchReviews()
        }
    }

    private fun deleteScore(workId: String) {
        val userId = _state.value.currentUserId ?: return
        val review = _state.value.review(workId) ?: return
        val existing = review.scoreOf(userId) ?: return
        if (existing.id == MemberScore.PENDING_ID) {
            if (_state.value.detail?.takeIf { it.workId == workId }?.isSavingScore == true) {
                // Optimistic score from a save still in flight; there's no server id to delete yet.
                message("Still saving your score. Try again in a moment.")
                return
            }
            // A score queued offline that never reached the server: just forget it.
            updateReview(workId) { it.withScore(userId, null) }
            viewModelScope.launch {
                reviewsRepository.discardQueuedScore(clubSlug, workId)
                message("Your score was removed")
            }
            return
        }
        // A poll left over from an earlier save would write server snapshots over the removal.
        scorePolls.remove(workId)?.cancel()
        updateReview(workId) { it.withScore(userId, null) }
        viewModelScope.launch {
            reviewsRepository.deleteScore(clubSlug, existing.id)
                // The optimistic removal already recomputed the average.
                .onSuccess { message("Your score was removed") }
                .onError { error ->
                    updateReview(workId) { review }
                    emitError(error, "remove your score")
                    fetchReviews()
                }
        }
    }

    private fun generateQuestions() {
        val workId = _state.value.detail?.workId ?: return
        updateDetail(workId) { it.copy(questions = it.questions.copy(isLoading = true)) }
        viewModelScope.launch {
            when (val result = reviewsRepository.generateDiscussionQuestions(clubSlug, workId)) {
                is Result.Success -> updateDetail(workId) {
                    it.copy(questions = DiscussionQuestionsState(questions = result.data))
                }
                is Result.Error -> updateDetail(workId) {
                    it.copy(questions = it.questions.copy(isLoading = false, failed = true))
                }
            }
        }
    }

    private fun message(text: String) {
        _events.tryEmit(ReviewsEvent.Message(UiMessage(text)))
    }

    private fun emitError(error: DataError.Remote, action: String, retry: (() -> Unit)? = null) {
        _events.tryEmit(ReviewsEvent.Message(error.toUiMessage(action, retry)))
    }
}

private const val PENDING_WORK_PREFIX = "pending-work:"
private const val PENDING_EXPIRY_MS = 15_000L
private const val SCORE_POLL_ATTEMPTS = 10
private const val SCORE_POLL_INTERVAL_MS = 500L

private const val KEY_QUERY = "reviews_query"
private const val KEY_DETAIL = "reviews_detail_work_id"
