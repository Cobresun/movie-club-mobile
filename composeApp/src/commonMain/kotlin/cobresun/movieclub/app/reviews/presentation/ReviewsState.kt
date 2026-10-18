package cobresun.movieclub.app.reviews.presentation

import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.filters.domain.FilterQuery
import cobresun.movieclub.app.filters.domain.FilterResults
import cobresun.movieclub.app.filters.domain.FilterTarget
import cobresun.movieclub.app.filters.domain.computeFilters
import cobresun.movieclub.app.reviews.domain.ScoredWork
import kotlinx.datetime.LocalDate
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewFact
import cobresun.movieclub.app.reviews.domain.ReviewFacts
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ScoreReveal
import cobresun.movieclub.app.reviews.domain.sortReviews
import cobresun.movieclub.app.core.presentation.DerivedMemo

/* What the Reviews tab shows (ReviewsState), what it can do (ReviewsAction) and its one-off events. */

data class ReviewsState(
    val clubSlug: String? = null,
    val clubType: ClubType = ClubType.MOVIE,
    val clubName: String = "",
    val reviews: AsyncResult<List<Review>> = AsyncResult.Loading,
    val isRefreshing: Boolean = false,
    /** The search text plus the filters picked in the filters sheet. */
    val filterQuery: FilterQuery = FilterQuery(),
    val sortState: ReviewSortState = ReviewSortState(),
    val members: List<Member> = emptyList(),
    val currentUserId: String? = null,
    val discussionQuestionsEnabled: Boolean = false,
    val detail: ReviewDetailState? = null,
    /** The user's scores across all clubs, for Score Assist. */
    val myScoredWorks: List<ScoredWork> = emptyList(),
    /** Works whose club scores were revealed by hand this visit (web keeps these in memory too). */
    val revealedWorkIds: Set<String> = emptySet(),
    /** The club's Up Next pointer and every user-list item, to find the pinned work. */
    val upNextWorkId: String? = null,
    val listedWorks: List<ListedWork> = emptyList(),
    /** The Up Next work being moved into reviews; keeps its card up, pending, until it lands. */
    val startingUpNext: ListedWork? = null,
    /** Works on their way onto the reviews list: shown as pending cards until the refetch lands. */
    val pendingWorks: List<Work> = emptyList(),
    /** Derived values, kept across copies; see [DerivedMemo]. */
    private val memo: DerivedMemo = DerivedMemo(),
) {
    /**
     * The Up Next card's work, or null when nothing is pinned or it's already
     * reviewed. `all-items` is in list order, so the first match is the topmost list's copy.
     */
    val upNext: ListedWork?
        get() = startingUpNext ?: upNextWorkId?.let { id ->
            val reviews = (reviews as? AsyncResult.Success)?.data ?: return null
            val reviewed = memo.get("reviewed-ids", reviews) { reviews.mapTo(HashSet()) { it.work.id } }
            if (id in reviewed) null else listedWorks.firstOrNull { it.work.id == id }
        }

    fun isRevealed(review: Review): Boolean = ScoreReveal.isRevealed(review, currentUserId, revealedWorkIds)

    fun canReveal(review: Review): Boolean = ScoreReveal.canReveal(review, currentUserId, revealedWorkIds)

    /** Filter bar state for the reviews: offered filters, facets, chips and the matches. */
    val filters: FilterResults<Review>
        get() = memo.get("filters", reviews, filterQuery, clubType) {
        computeFilters(
            items = (reviews as? AsyncResult.Success)?.data.orEmpty(),
            query = filterQuery,
            clubType = clubType,
            id = { it.work.id },
            target = { FilterTarget(it.work, it.average) },
        )
        }

    val visibleReviews: List<Review>
        get() = filters.let { f -> memo.get("visible", f, sortState) { sortReviews(f.filtered, sortState) } }

    val reviewedExternalIds: Set<String>
        get() = memo.get("reviewed-external", reviews) {
            (reviews as? AsyncResult.Success)?.data.orEmpty().mapNotNull { it.work.externalId }.toSet()
        }

    /**
     * Spotlight fact for [review], or null while its club scores are still hidden:
     * like web, every fact is derived from the scores, so it shares their reveal.
     */
    fun spotlightFact(review: Review): ReviewFact? {
        if (!isRevealed(review)) return null
        // Sorts and scans every review; reuse it until the reviews actually change.
        return memo.get("fact-${review.work.id}", reviews) {
            ReviewFacts.compute((reviews as? AsyncResult.Success)?.data.orEmpty(), review.work.id)
        }
    }

    fun review(workId: String): Review? = (reviews as? AsyncResult.Success)?.data?.firstOrNull { it.work.id == workId }
}

data class ReviewDetailState(
    val workId: String,
    /** Full metadata incl. cast; falls back to the list summary while loading. */
    val details: AsyncResult<WorkDetails?> = AsyncResult.Loading,
    val thread: CommentsState = CommentsState(),
    val questions: DiscussionQuestionsState = DiscussionQuestionsState(),
    val isSavingScore: Boolean = false,
)

/**
 * AI discussion questions (web `DiscussionQuestions.vue`). [questions] is null
 * until generated; an empty list means the model didn't recognize the work.
 * A failed regenerate keeps the previous questions and sets [failed].
 */
data class DiscussionQuestionsState(
    val questions: List<String>? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
)

sealed interface ReviewsEvent {
    data class Message(val message: UiMessage) : ReviewsEvent

    /** Open the system share sheet for a public link. */
    data class Share(val url: String, val title: String) : ReviewsEvent
}

sealed interface ReviewsAction {
    data object Refresh : ReviewsAction
    /** The tab became visible: Up Next may have changed on the Lists tab. */
    data object Shown : ReviewsAction
    data object StartUpNextReview : ReviewsAction
    /** New search text and/or filter picks. */
    data class Filter(val query: FilterQuery) : ReviewsAction
    data class Sort(val option: ReviewSortOption) : ReviewsAction
    data object ToggleSortDirection : ReviewsAction

    data class AddReview(val work: NewWork) : ReviewsAction
    /** A work another tab is moving into reviews (e.g. picked from a list). */
    data class ShowPending(val work: Work) : ReviewsAction
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
