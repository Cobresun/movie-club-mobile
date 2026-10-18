package cobresun.movieclub.app.reviews.data.repository

import cobresun.movieclub.app.core.data.OfflineKeys
import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.readThrough
import cobresun.movieclub.app.core.data.saved
import cobresun.movieclub.app.reviews.data.dto.CommentDto
import cobresun.movieclub.app.reviews.data.PendingScore
import cobresun.movieclub.app.reviews.data.ScoreOutbox
import cobresun.movieclub.app.reviews.data.applyPendingScores
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.ScoreSave
import cobresun.movieclub.app.reviews.data.dto.MemberScoredWorkDto
import cobresun.movieclub.app.reviews.data.dto.ReviewItemDto
import kotlinx.serialization.builtins.ListSerializer
import cobresun.movieclub.app.core.data.mappers.toWorkDetails
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.reviews.data.dto.CommentBodyDto
import cobresun.movieclub.app.reviews.data.dto.AddedDateBodyDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import cobresun.movieclub.app.reviews.data.dto.ListInsertBodyDto
import cobresun.movieclub.app.reviews.data.dto.ScoreCreateDto
import cobresun.movieclub.app.reviews.data.dto.ScoreUpdateDto
import cobresun.movieclub.app.reviews.data.mappers.toReview
import cobresun.movieclub.app.reviews.data.mappers.toWorkScores
import cobresun.movieclub.app.reviews.data.mappers.toSharedReview
import cobresun.movieclub.app.reviews.domain.SharedReview
import cobresun.movieclub.app.reviews.domain.WorkScores
import cobresun.movieclub.app.reviews.data.mappers.toScoredWork
import cobresun.movieclub.app.reviews.domain.ScoredWork
import cobresun.movieclub.app.reviews.data.mappers.toReviewComment
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.domain.ScoreScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReviewsRepositoryImpl(
    private val dataSource: ReviewsDataSource,
    private val caches: SessionCaches,
    private val outbox: ScoreOutbox,
) : ReviewsRepository, ReviewCommentsRepository {
    private suspend fun reviewsListId(clubSlug: String): Result<String, DataError.Remote> =
        caches.reviewsListId.get(clubSlug) { dataSource.getReviewsListId(clubSlug).map { it.id } }

    override suspend fun getMyScoredWorks(): Result<List<ScoredWork>, DataError.Remote> =
        caches.offline.readThrough(OfflineKeys.MY_SCORES, ListSerializer(MemberScoredWorkDto.serializer())) {
            dataSource.getMyScores()
        }.let { result ->
            // Mapping walks each work's JSON tree; keep it off the main thread.
            withContext(Dispatchers.Default) {
                val zone = TimeZone.currentSystemDefault()
                result.map { items -> items.map { it.toScoredWork(zone) } }
            }
        }

    override suspend fun getReviews(clubSlug: String, refresh: Boolean): Result<List<Review>, DataError.Remote> =
        caches.reviews.get(clubSlug, refresh) {
            caches.offline.readThrough(OfflineKeys.reviews(clubSlug), reviewItemsSerializer) { dataSource.getReviews(clubSlug) }
                .map { it.toReviews() }
        }.map { it.withQueuedScores(clubSlug) }

    override suspend fun savedReviews(clubSlug: String): List<Review>? = caches.reviews.saved(clubSlug) {
        caches.offline.load(OfflineKeys.reviews(clubSlug), reviewItemsSerializer)?.toReviews()
    }?.withQueuedScores(clubSlug)

    private val reviewItemsSerializer = ListSerializer(ReviewItemDto.serializer())

    private suspend fun List<ReviewItemDto>.toReviews(): List<Review> = withContext(Dispatchers.Default) {
        val zone = TimeZone.currentSystemDefault()
        map { it.toReview(zone) }.sortedByDescending { it.work.addedAt }
    }

    override suspend fun addReview(clubSlug: String, work: NewWork): Result<Unit, DataError.Remote> =
        reviewsListId(clubSlug).flatMap { listId ->
            dataSource.addToList(
                clubSlug,
                listId,
                ListInsertBodyDto(
                    type = work.type.value,
                    title = work.title,
                    externalId = work.externalId,
                    imageUrl = work.imageUrl,
                ),
            )
        }.onSuccess { caches.invalidateClubWorks(clubSlug) }

    override suspend fun removeReview(clubSlug: String, workId: String): Result<Unit, DataError.Remote> =
        reviewsListId(clubSlug).flatMap { listId -> dataSource.removeFromList(clubSlug, listId, workId) }
            .onSuccess { caches.invalidateClubWorks(clubSlug) }

    override suspend fun updateReviewedDate(clubSlug: String, workId: String, date: LocalDate): Result<Unit, DataError.Remote> =
        reviewsListId(clubSlug).flatMap { listId ->
            // Start of the local day, like the web's date editor.
            val iso = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
            dataSource.updateAddedDate(clubSlug, listId, workId, AddedDateBodyDto(iso))
        }.onSuccess { caches.reviews.invalidate(clubSlug) }

    override suspend fun saveScore(
        clubSlug: String,
        workId: String,
        userId: String,
        existingScoreId: String?,
        score: Double,
    ): Result<ScoreSave, DataError.Remote> {
        val clamped = ScoreScale.clamp(score)
        val result = if (existingScoreId == null || existingScoreId == MemberScore.PENDING_ID) {
            dataSource.createScore(clubSlug, ScoreCreateDto(score = clamped, workId = workId))
        } else {
            dataSource.updateScore(clubSlug, existingScoreId, ScoreUpdateDto(score = clamped))
        }
        return when {
            result is Result.Success -> {
                // This save supersedes one queued earlier for the work.
                outbox.remove(clubSlug, workId)
                caches.reviews.invalidate(clubSlug)
                Result.Success(ScoreSave.SAVED)
            }
            // No connection (or no answer: sending again is safe, it's a set): keep it and
            // send it on reconnect. Server errors aren't queued; the member can retry.
            result is Result.Error &&
                (result.error == DataError.Remote.NO_INTERNET || result.error == DataError.Remote.REQUEST_TIMEOUT) -> {
                outbox.put(PendingScore(clubSlug, workId, userId, clamped))
                Result.Success(ScoreSave.QUEUED)
            }
            else -> result.map { ScoreSave.SAVED }
        }
    }

    /**
     * Sends the scores queued offline (on reconnect and launch). Each checks the member's
     * current score first, so a score that already landed isn't created twice.
     * @return whether any reached the server.
     */
    suspend fun sendQueuedScores(): Boolean {
        val sent = outbox.sendAll { pending ->
            dataSource.getWorkScores(pending.clubSlug, pending.workId).flatMap { scores ->
                val mine = scores[pending.userId]
                when {
                    mine == null -> dataSource.createScore(pending.clubSlug, ScoreCreateDto(pending.score, pending.workId))
                    mine.score == pending.score -> Result.Success(Unit)
                    else -> dataSource.updateScore(pending.clubSlug, mine.id, ScoreUpdateDto(pending.score))
                }
            }
        }
        sent.map { it.clubSlug }.distinct().forEach { caches.reviews.invalidate(it) }
        return sent.isNotEmpty()
    }

    private suspend fun List<Review>.withQueuedScores(clubSlug: String): List<Review> =
        applyPendingScores(this, outbox.pending().filter { it.clubSlug == clubSlug })

    override suspend fun getSharedReview(clubSlug: String, workId: String): Result<SharedReview, DataError.Remote> =
        dataSource.getSharedReview(clubSlug, workId).map { it.toSharedReview() }

    override suspend fun getWorkScores(clubSlug: String, workId: String): Result<WorkScores, DataError.Remote> =
        dataSource.getWorkScores(clubSlug, workId).map { it.toWorkScores() }

    override suspend fun deleteScore(clubSlug: String, scoreId: String): Result<Unit, DataError.Remote> =
        dataSource.deleteScore(clubSlug, scoreId).onSuccess { caches.reviews.invalidate(clubSlug) }

    override suspend fun discardQueuedScore(clubSlug: String, workId: String) = outbox.remove(clubSlug, workId)

    override suspend fun generateDiscussionQuestions(
        clubSlug: String,
        workId: String,
    ): Result<List<String>, DataError.Remote> =
        dataSource.generateDiscussionQuestions(clubSlug, workId).map { it.questions }

    override suspend fun getComments(clubSlug: String, workId: String): Result<List<ReviewComment>, DataError.Remote> =
        caches.offline.readThrough(OfflineKeys.comments(clubSlug, workId), ListSerializer(CommentDto.serializer())) {
            dataSource.getComments(clubSlug, workId)
        }.map { list -> list.map { it.toReviewComment() } }

    override suspend fun addComment(
        clubSlug: String,
        workId: String,
        content: String,
        spoiler: Boolean,
    ): Result<Unit, DataError.Remote> =
        dataSource.addComment(clubSlug, workId, CommentBodyDto(content.trim(), spoiler))

    override suspend fun editComment(
        clubSlug: String,
        workId: String,
        commentId: String,
        content: String,
        spoiler: Boolean,
    ): Result<Unit, DataError.Remote> =
        dataSource.editComment(clubSlug, workId, commentId, CommentBodyDto(content.trim(), spoiler))

    override suspend fun deleteComment(clubSlug: String, workId: String, commentId: String): Result<Unit, DataError.Remote> =
        dataSource.deleteComment(clubSlug, workId, commentId)
}
