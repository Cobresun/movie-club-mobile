package cobresun.movieclub.app.reviews.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.domain.ShareLinks
import cobresun.movieclub.app.reviews.domain.ScoredWork
import kotlinx.datetime.LocalDate
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.isRetryable
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewFact
import cobresun.movieclub.app.reviews.domain.ReviewFacts
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.filterReviews
import cobresun.movieclub.app.reviews.domain.sortReviews
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewsState(
    val clubType: ClubType = ClubType.MOVIE,
    val clubName: String = "",
    val reviews: AsyncResult<List<Review>> = AsyncResult.Loading,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val sortState: ReviewSortState = ReviewSortState(),
    val members: List<Member> = emptyList(),
    val currentUserId: String? = null,
    val discussionQuestionsEnabled: Boolean = false,
    val detail: ReviewDetailState? = null,
    /** The user's scores across all clubs, for Score Assist. */
    val myScoredWorks: List<ScoredWork> = emptyList(),
    /** Works whose club scores were revealed by hand this visit (web keeps these in memory too). */
    val revealedWorkIds: Set<String> = emptySet(),
) {
    fun isRevealed(review: Review): Boolean = ScoreReveal.isRevealed(review, currentUserId, revealedWorkIds)

    fun canReveal(review: Review): Boolean = ScoreReveal.canReveal(review, currentUserId, revealedWorkIds)

    val visibleReviews: List<Review>
        get() = sortReviews(
            filterReviews((reviews as? AsyncResult.Success)?.data.orEmpty(), query),
            sortState,
        )

    val reviewedExternalIds: Set<String>
        get() = (reviews as? AsyncResult.Success)?.data.orEmpty().mapNotNull { it.work.externalId }.toSet()

    /**
     * Spotlight fact for [review], or null while its club scores are still hidden:
     * like web, every fact is derived from the scores, so it shares their reveal.
     */
    fun spotlightFact(review: Review): ReviewFact? {
        if (!isRevealed(review)) return null
        return ReviewFacts.compute((reviews as? AsyncResult.Success)?.data.orEmpty(), review.work.id)
    }

    fun review(workId: String): Review? = (reviews as? AsyncResult.Success)?.data?.firstOrNull { it.work.id == workId }
}

data class ReviewDetailState(
    val workId: String,
    /** Full metadata incl. cast; falls back to the list summary while loading. */
    val details: AsyncResult<WorkDetails?> = AsyncResult.Loading,
    val comments: AsyncResult<List<ReviewComment>> = AsyncResult.Loading,
    val questions: AsyncResult<List<String>>? = null,
    val isSavingScore: Boolean = false,
    val isPostingComment: Boolean = false,
)

sealed interface ReviewsEvent {
    data class Message(val message: UiMessage) : ReviewsEvent

    /** Open the system share sheet for a public link. */
    data class Share(val url: String, val title: String) : ReviewsEvent
}

sealed interface ReviewsAction {
    data object Refresh : ReviewsAction
    data class Search(val query: String) : ReviewsAction
    data class Sort(val option: ReviewSortOption) : ReviewsAction
    data object ToggleSortDirection : ReviewsAction

    data class AddReview(val work: NewWork) : ReviewsAction
    data class RemoveReview(val work: Work) : ReviewsAction
    data class Share(val work: Work) : ReviewsAction

    data class OpenDetail(val workId: String) : ReviewsAction
    data object CloseDetail : ReviewsAction
    data class SaveScore(val workId: String, val score: Double) : ReviewsAction
    data class SetReviewedDate(val workId: String, val date: LocalDate) : ReviewsAction
    data class DeleteScore(val workId: String) : ReviewsAction
    data class RevealScores(val workId: String) : ReviewsAction
    data class AddComment(val content: String, val spoiler: Boolean) : ReviewsAction
    data class EditComment(val commentId: String, val content: String, val spoiler: Boolean) : ReviewsAction
    data class DeleteComment(val commentId: String) : ReviewsAction
    data object GenerateQuestions : ReviewsAction
    data object RetryComments : ReviewsAction
}

class ReviewsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val commentsRepository: ReviewCommentsRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val clubRepository: ClubRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    // The search text and open details sheet survive process death like the selected tab does.
    private val _state = MutableStateFlow(
        ReviewsState(
            query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
            detail = savedStateHandle.get<String>(KEY_DETAIL)?.let { ReviewDetailState(it) },
        )
    )
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<ReviewsEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        loadContext()
        loadReviews(initial = true)
        _state.value.detail?.let { loadDetail(it.workId) }
    }

    fun onAction(action: ReviewsAction) {
        when (action) {
            ReviewsAction.Refresh -> {
                if (_state.value.clubName.isBlank()) loadContext()
                loadReviews(initial = false)
                _state.value.detail?.let { loadDetail(it.workId) }
            }

            is ReviewsAction.Search -> {
                savedStateHandle[KEY_QUERY] = action.query
                _state.update { it.copy(query = action.query) }
            }
            is ReviewsAction.Sort -> _state.update { s ->
                val option = if (s.sortState.option == action.option) ReviewSortOption.None else action.option
                s.copy(sortState = ReviewSortState(option, option.defaultDescending))
            }

            ReviewsAction.ToggleSortDirection -> _state.update {
                it.copy(sortState = it.sortState.copy(descending = !it.sortState.descending))
            }

            is ReviewsAction.AddReview -> addReview(action.work)
            is ReviewsAction.RemoveReview -> removeReview(action.work)
            is ReviewsAction.Share -> share(action.work)
            ReviewsAction.RetryComments -> _state.value.detail?.let { d ->
                updateDetail(d.workId) { it.copy(comments = AsyncResult.Loading) }
                loadComments(d.workId)
            }

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
            is ReviewsAction.AddComment -> addComment(action.content, action.spoiler)
            is ReviewsAction.EditComment -> editComment(action.commentId, action.content, action.spoiler)
            is ReviewsAction.DeleteComment -> deleteComment(action.commentId)
            ReviewsAction.GenerateQuestions -> generateQuestions()
        }
    }

    /** Called when a work arrives from the lists tab. */
    fun refreshQuietly() = loadReviews(initial = false)

    private fun loadContext() {
        viewModelScope.launch {
            memberRepository.getClubs().onSuccess { clubs ->
                clubs.firstOrNull { it.slug == clubSlug }?.let { club -> _state.update { it.copy(clubType = club.type, clubName = club.name) } }
            }
        }
        viewModelScope.launch {
            // `/api/member` no longer exists; the BetterAuth session identifies the signed-in user.
            authRepository.getUser().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { members -> _state.update { it.copy(members = members) } }
        }
        loadMyScoredWorks()
        viewModelScope.launch {
            reviewsRepository.isDiscussionQuestionsEnabled(clubSlug)
                .onSuccess { enabled -> _state.update { it.copy(discussionQuestionsEnabled = enabled) } }
        }
    }

    private fun loadMyScoredWorks() {
        viewModelScope.launch {
            reviewsRepository.getMyScoredWorks().onSuccess { works -> _state.update { it.copy(myScoredWorks = works) } }
        }
    }

    private fun loadReviews(initial: Boolean) {
        viewModelScope.launch {
            if (initial) _state.update { it.copy(reviews = AsyncResult.Loading) }
            else _state.update { it.copy(isRefreshing = true) }
            when (val result = reviewsRepository.getReviews(clubSlug)) {
                is Result.Success -> _state.update { it.copy(reviews = AsyncResult.Success(result.data)) }
                is Result.Error -> if (_state.value.reviews is AsyncResult.Success) {
                    emitError(result.error, "refresh reviews") { loadReviews(initial = false) }
                } else {
                    _state.update { it.copy(reviews = AsyncResult.Error(dataError = result.error)) }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadDetail(workId: String) {
        viewModelScope.launch {
            val result = reviewsRepository.getWorkDetails(clubSlug, workId)
            updateDetail(workId) {
                it.copy(
                    details = when (result) {
                        is Result.Success -> AsyncResult.Success(result.data)
                        is Result.Error -> AsyncResult.Error(dataError = result.error)
                    }
                )
            }
        }
        loadComments(workId)
    }

    private fun loadComments(workId: String) {
        viewModelScope.launch {
            val result = commentsRepository.getComments(clubSlug, workId)
            updateDetail(workId) {
                it.copy(
                    comments = when (result) {
                        is Result.Success -> AsyncResult.Success(result.data)
                        is Result.Error -> if (it.comments is AsyncResult.Success) it.comments
                        else AsyncResult.Error(dataError = result.error)
                    }
                )
            }
        }
    }

    private fun updateDetail(workId: String, transform: (ReviewDetailState) -> ReviewDetailState) {
        _state.update { s -> if (s.detail?.workId == workId) s.copy(detail = transform(s.detail)) else s }
    }

    private fun updateReview(workId: String, transform: (Review) -> Review) {
        _state.update { s ->
            val list = (s.reviews as? AsyncResult.Success)?.data ?: return@update s
            s.copy(reviews = AsyncResult.Success(list.map { if (it.work.id == workId) transform(it) else it }))
        }
    }

    private fun addReview(work: NewWork) {
        viewModelScope.launch {
            reviewsRepository.addReview(clubSlug, work)
                .onSuccess {
                    message("Added ${work.title} to reviews")
                    loadReviews(initial = false)
                }
                .onError { error ->
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
        updateReview(workId) { it.withScore(userId, MemberScore(existing?.id ?: PENDING_SCORE_ID, userId, score)) }
        updateDetail(workId) { it.copy(isSavingScore = true) }
        viewModelScope.launch {
            reviewsRepository.saveScore(clubSlug, workId, existing?.id, score)
                .onSuccess {
                    message("Score saved: ${ScoreScale.format(score)}")
                    loadMyScoredWorks()
                }
                .onError { error ->
                    updateReview(workId) { review }
                    emitError(error, "save your score") { saveScore(workId, score) }
                }
            updateDetail(workId) { it.copy(isSavingScore = false) }
            loadReviews(initial = false) // pick up the server's score id & average
        }
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
            loadReviews(initial = false)
        }
    }

    private fun deleteScore(workId: String) {
        val userId = _state.value.currentUserId ?: return
        val review = _state.value.review(workId) ?: return
        val existing = review.scoreOf(userId) ?: return
        if (existing.id == PENDING_SCORE_ID) {
            // Optimistic score from a save still in flight; there's no server id to delete yet.
            message("Still saving your score. Try again in a moment.")
            return
        }
        updateReview(workId) { it.withScore(userId, null) }
        viewModelScope.launch {
            reviewsRepository.deleteScore(clubSlug, existing.id)
                .onSuccess { message("Your score was removed") }
                .onError { error ->
                    updateReview(workId) { review }
                    emitError(error, "remove your score")
                }
            loadReviews(initial = false)
        }
    }

    private fun addComment(content: String, spoiler: Boolean) {
        val workId = _state.value.detail?.workId ?: return
        if (content.isBlank()) return
        updateDetail(workId) { it.copy(isPostingComment = true) }
        viewModelScope.launch {
            commentsRepository.addComment(clubSlug, workId, content, spoiler)
                .onSuccess { loadComments(workId) }
                .onError { emitError(it, "post your comment") }
            updateDetail(workId) { it.copy(isPostingComment = false) }
        }
    }

    private fun editComment(commentId: String, content: String, spoiler: Boolean) {
        val workId = _state.value.detail?.workId ?: return
        viewModelScope.launch {
            commentsRepository.editComment(clubSlug, workId, commentId, content, spoiler)
                .onSuccess { loadComments(workId) }
                .onError { emitError(it, "edit your comment") }
        }
    }

    private fun deleteComment(commentId: String) {
        val workId = _state.value.detail?.workId ?: return
        val before = _state.value.detail?.comments
        updateDetail(workId) { d ->
            d.copy(comments = (d.comments as? AsyncResult.Success)?.let { c -> AsyncResult.Success(c.data.filter { it.id != commentId }) } ?: d.comments)
        }
        viewModelScope.launch {
            commentsRepository.deleteComment(clubSlug, workId, commentId)
                .onError { error ->
                    if (before != null) updateDetail(workId) { it.copy(comments = before) }
                    emitError(error, "delete your comment")
                }
        }
    }

    private fun generateQuestions() {
        val workId = _state.value.detail?.workId ?: return
        updateDetail(workId) { it.copy(questions = AsyncResult.Loading) }
        viewModelScope.launch {
            when (val result = reviewsRepository.generateDiscussionQuestions(clubSlug, workId)) {
                is Result.Success -> updateDetail(workId) { it.copy(questions = AsyncResult.Success(result.data)) }
                is Result.Error -> {
                    updateDetail(workId) { it.copy(questions = null) }
                    emitError(result.error, "generate discussion questions") { generateQuestions() }
                }
            }
        }
    }

    private fun message(text: String) {
        _events.tryEmit(ReviewsEvent.Message(UiMessage(text)))
    }

    private fun emitError(error: DataError.Remote, action: String, retry: (() -> Unit)? = null) {
        val canRetry = retry != null && error.isRetryable
        _events.tryEmit(
            ReviewsEvent.Message(
                UiMessage(
                    text = error.toUserMessage(action),
                    actionLabel = if (canRetry) "Retry" else null,
                    onAction = if (canRetry) retry else null,
                )
            )
        )
    }
}

private const val PENDING_SCORE_ID = "pending"

private const val KEY_QUERY = "reviews_query"
private const val KEY_DETAIL = "reviews_detail_work_id"
