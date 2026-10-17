package cobresun.movieclub.app.reviews.data.repository

import cobresun.movieclub.app.core.data.mappers.toWorkDetails
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map
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

class ReviewsRepositoryImpl(
    private val dataSource: ReviewsDataSource,
) : ReviewsRepository, ReviewCommentsRepository {
    private val reviewsListIds = mutableMapOf<String, String>()

    private suspend fun reviewsListId(clubSlug: String): Result<String, DataError.Remote> {
        reviewsListIds[clubSlug]?.let { return Result.Success(it) }
        return dataSource.getReviewsListId(clubSlug).map { dto ->
            reviewsListIds[clubSlug] = dto.id
            dto.id
        }
    }

    override suspend fun getMyScoredWorks(): Result<List<ScoredWork>, DataError.Remote> =
        dataSource.getMyScores().map { items -> items.map { it.toScoredWork() } }

    override suspend fun getReviews(clubSlug: String): Result<List<Review>, DataError.Remote> =
        dataSource.getReviews(clubSlug).map { items ->
            items.map { it.toReview() }.sortedByDescending { it.work.addedAt }
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
        }

    override suspend fun removeReview(clubSlug: String, workId: String): Result<Unit, DataError.Remote> =
        reviewsListId(clubSlug).flatMap { listId -> dataSource.removeFromList(clubSlug, listId, workId) }

    override suspend fun updateReviewedDate(clubSlug: String, workId: String, date: LocalDate): Result<Unit, DataError.Remote> =
        reviewsListId(clubSlug).flatMap { listId ->
            // Start of the local day, like the web's date editor.
            val iso = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
            dataSource.updateAddedDate(clubSlug, listId, workId, AddedDateBodyDto(iso))
        }

    override suspend fun saveScore(
        clubSlug: String,
        workId: String,
        existingScoreId: String?,
        score: Double,
    ): Result<Unit, DataError.Remote> {
        val clamped = ScoreScale.clamp(score)
        return if (existingScoreId == null) {
            dataSource.createScore(clubSlug, ScoreCreateDto(score = clamped, workId = workId))
        } else {
            dataSource.updateScore(clubSlug, existingScoreId, ScoreUpdateDto(score = clamped))
        }
    }

    override suspend fun getSharedReview(clubSlug: String, workId: String): Result<SharedReview, DataError.Remote> =
        dataSource.getSharedReview(clubSlug, workId).map { it.toSharedReview() }

    override suspend fun getWorkScores(clubSlug: String, workId: String): Result<WorkScores, DataError.Remote> =
        dataSource.getWorkScores(clubSlug, workId).map { it.toWorkScores() }

    override suspend fun deleteScore(clubSlug: String, scoreId: String): Result<Unit, DataError.Remote> =
        dataSource.deleteScore(clubSlug, scoreId)

    override suspend fun isDiscussionQuestionsEnabled(clubSlug: String): Result<Boolean, DataError.Remote> =
        dataSource.getSettings(clubSlug).map { it.features.discussionQuestions }

    override suspend fun generateDiscussionQuestions(
        clubSlug: String,
        workId: String,
    ): Result<List<String>, DataError.Remote> =
        dataSource.generateDiscussionQuestions(clubSlug, workId).map { it.questions }

    override suspend fun getComments(clubSlug: String, workId: String): Result<List<ReviewComment>, DataError.Remote> =
        dataSource.getComments(clubSlug, workId).map { list -> list.map { it.toReviewComment() } }

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
