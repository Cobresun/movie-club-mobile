package cobresun.movieclub.app.reviews.data.network

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.dto.CommentBodyDto
import cobresun.movieclub.app.reviews.data.dto.CommentDto
import cobresun.movieclub.app.reviews.data.dto.DiscussionQuestionsDto
import cobresun.movieclub.app.reviews.data.dto.ListInsertBodyDto
import cobresun.movieclub.app.reviews.data.dto.AddedDateBodyDto
import cobresun.movieclub.app.reviews.data.dto.MemberScoredWorkDto
import cobresun.movieclub.app.reviews.data.dto.ReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ReviewsListIdDto
import cobresun.movieclub.app.reviews.data.dto.ScoreCreateDto
import cobresun.movieclub.app.reviews.data.dto.ScoreDto
import cobresun.movieclub.app.reviews.data.dto.SharedReviewDto
import cobresun.movieclub.app.reviews.data.dto.ScoreUpdateDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonElement

interface ReviewsDataSource {
    suspend fun getReviews(clubSlug: String): Result<List<ReviewItemDto>, DataError.Remote>
    suspend fun updateAddedDate(clubSlug: String, listId: String, workId: String, body: AddedDateBodyDto): Result<Unit, DataError.Remote>
    suspend fun getMyScores(): Result<List<MemberScoredWorkDto>, DataError.Remote>
    suspend fun getReviewsListId(clubSlug: String): Result<ReviewsListIdDto, DataError.Remote>
    suspend fun addToList(clubSlug: String, listId: String, body: ListInsertBodyDto): Result<Unit, DataError.Remote>
    suspend fun removeFromList(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote>
    suspend fun createScore(clubSlug: String, body: ScoreCreateDto): Result<Unit, DataError.Remote>
    suspend fun updateScore(clubSlug: String, scoreId: String, body: ScoreUpdateDto): Result<Unit, DataError.Remote>
    suspend fun deleteScore(clubSlug: String, scoreId: String): Result<Unit, DataError.Remote>
    suspend fun getSharedReview(clubSlug: String, workId: String): Result<SharedReviewDto, DataError.Remote>
    suspend fun getWorkScores(clubSlug: String, workId: String): Result<Map<String, ScoreDto>, DataError.Remote>
    suspend fun generateDiscussionQuestions(clubSlug: String, workId: String): Result<DiscussionQuestionsDto, DataError.Remote>
    suspend fun getComments(clubSlug: String, workId: String): Result<List<CommentDto>, DataError.Remote>
    suspend fun addComment(clubSlug: String, workId: String, body: CommentBodyDto): Result<Unit, DataError.Remote>
    suspend fun editComment(clubSlug: String, workId: String, commentId: String, body: CommentBodyDto): Result<Unit, DataError.Remote>
    suspend fun deleteComment(clubSlug: String, workId: String, commentId: String): Result<Unit, DataError.Remote>
}

class KtorReviewsDataSource(
    private val httpClient: HttpClient,
) : ReviewsDataSource {
    private fun club(slug: String) = "$BASE_URL/api/club/$slug"
    private fun reviews(slug: String) = "${club(slug)}/reviews"

    override suspend fun getReviews(clubSlug: String): Result<List<ReviewItemDto>, DataError.Remote> =
        safeCall { httpClient.get("${club(clubSlug)}/list/reviews") }

    override suspend fun updateAddedDate(clubSlug: String, listId: String, workId: String, body: AddedDateBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${club(clubSlug)}/list/$listId/items/$workId/added-date") { setBody(body) } }

    override suspend fun getMyScores(): Result<List<MemberScoredWorkDto>, DataError.Remote> =
        safeCall { httpClient.get("$BASE_URL/api/member/scores") }

    override suspend fun getReviewsListId(clubSlug: String): Result<ReviewsListIdDto, DataError.Remote> =
        safeCall { httpClient.get("${club(clubSlug)}/list/reviews-id") }

    override suspend fun addToList(clubSlug: String, listId: String, body: ListInsertBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${club(clubSlug)}/list/$listId/items") { setBody(body) } }

    override suspend fun removeFromList(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${club(clubSlug)}/list/$listId/items/$workId") }

    override suspend fun createScore(clubSlug: String, body: ScoreCreateDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post(reviews(clubSlug)) { setBody(body) } }

    override suspend fun updateScore(clubSlug: String, scoreId: String, body: ScoreUpdateDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${reviews(clubSlug)}/$scoreId") { setBody(body) } }

    override suspend fun deleteScore(clubSlug: String, scoreId: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${reviews(clubSlug)}/$scoreId") }

    override suspend fun getSharedReview(clubSlug: String, workId: String): Result<SharedReviewDto, DataError.Remote> =
        safeCall { httpClient.get("${reviews(clubSlug)}/$workId/shared") }

    override suspend fun getWorkScores(clubSlug: String, workId: String): Result<Map<String, ScoreDto>, DataError.Remote> =
        safeCall { httpClient.get("${reviews(clubSlug)}/$workId/scores") }

    override suspend fun generateDiscussionQuestions(
        clubSlug: String,
        workId: String,
    ): Result<DiscussionQuestionsDto, DataError.Remote> =
        safeCall { httpClient.post("${reviews(clubSlug)}/$workId/discussion-questions") }

    override suspend fun getComments(clubSlug: String, workId: String): Result<List<CommentDto>, DataError.Remote> =
        safeCall { httpClient.get("${reviews(clubSlug)}/$workId/comments") }

    override suspend fun addComment(clubSlug: String, workId: String, body: CommentBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${reviews(clubSlug)}/$workId/comments") { setBody(body) } }

    override suspend fun editComment(
        clubSlug: String,
        workId: String,
        commentId: String,
        body: CommentBodyDto,
    ): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${reviews(clubSlug)}/$workId/comments/$commentId") { setBody(body) } }

    override suspend fun deleteComment(clubSlug: String, workId: String, commentId: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${reviews(clubSlug)}/$workId/comments/$commentId") }
}
