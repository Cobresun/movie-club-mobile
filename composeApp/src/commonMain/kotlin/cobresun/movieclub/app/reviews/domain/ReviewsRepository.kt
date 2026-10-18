package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkDetails
import kotlinx.datetime.LocalDate

/**
 * Reviewed works, member scores, comments and discussion prompts for a club.
 * All operations address the club by slug.
 */
interface ReviewsRepository {
    /**
     * Reviewed works, newest first, with every current member's score. Tabs asking within
     * a minute share one answer; [refresh] asks the server again. Writes here renew it.
     */
    suspend fun getReviews(clubSlug: String, refresh: Boolean = false): Result<List<Review>, DataError.Remote>

    /**
     * The reviews last kept on this device, without a request (null when none): paint them,
     * then call [getReviews]. Offline, [getReviews] returns the same kept answer.
     */
    suspend fun savedReviews(clubSlug: String): List<Review>? = null

    /** Adds a work (from search) to the club's reviews. */
    suspend fun addReview(clubSlug: String, work: NewWork): Result<Unit, DataError.Remote>

    /** Removes a work from reviews for the whole club. */
    suspend fun removeReview(clubSlug: String, workId: String): Result<Unit, DataError.Remote>

    /** Changes the date a work was reviewed (its added date on the reviews list). */
    suspend fun updateReviewedDate(clubSlug: String, workId: String, date: LocalDate): Result<Unit, DataError.Remote>

    /**
     * Creates (when [existingScoreId] is null) or updates the signed-in member's ([userId])
     * score (0–10). Offline, the score is kept on this device and sent when the connection
     * returns ([ScoreSave.QUEUED]); until then [getReviews] shows it.
     */
    suspend fun saveScore(
        clubSlug: String,
        workId: String,
        userId: String,
        existingScoreId: String?,
        score: Double,
    ): Result<ScoreSave, DataError.Remote>

    /** A review as its public share link shows it; works for clubs you're not in. */
    suspend fun getSharedReview(clubSlug: String, workId: String): Result<SharedReview, DataError.Remote>

    /** Just one work's scores: a cheap poll for other members' scores landing. */
    suspend fun getWorkScores(clubSlug: String, workId: String): Result<WorkScores, DataError.Remote>

    /** Deletes one of the signed-in member's own scores. */
    suspend fun deleteScore(clubSlug: String, scoreId: String): Result<Unit, DataError.Remote>

    /** Forgets a score queued offline for [workId] (the member removed it before it was sent). */
    suspend fun discardQueuedScore(clubSlug: String, workId: String) {}

    /** Every work the signed-in member has scored, across all their clubs (for Score Assist). */
    suspend fun getMyScoredWorks(): Result<List<ScoredWork>, DataError.Remote>

    /** Generates up to five discussion questions for a work. */
    suspend fun generateDiscussionQuestions(clubSlug: String, workId: String): Result<List<String>, DataError.Remote>
}

/** Comments members leave on a reviewed work. Only the author may edit or delete. */
interface ReviewCommentsRepository {
    suspend fun getComments(clubSlug: String, workId: String): Result<List<ReviewComment>, DataError.Remote>
    suspend fun addComment(clubSlug: String, workId: String, content: String, spoiler: Boolean): Result<Unit, DataError.Remote>
    suspend fun editComment(
        clubSlug: String,
        workId: String,
        commentId: String,
        content: String,
        spoiler: Boolean,
    ): Result<Unit, DataError.Remote>

    suspend fun deleteComment(clubSlug: String, workId: String, commentId: String): Result<Unit, DataError.Remote>
}

enum class ScoreSave {
    /** The server has it. */
    SAVED,

    /** Offline: kept on this device and sent when the connection returns. */
    QUEUED,
}
