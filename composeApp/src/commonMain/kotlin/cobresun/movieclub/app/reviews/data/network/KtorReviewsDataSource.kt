package cobresun.movieclub.app.reviews.data.network

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.dto.NewReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ReviewDto
import cobresun.movieclub.app.reviews.data.dto.ScoreSubmissionDto
import cobresun.movieclub.app.reviews.data.dto.ScoreUpdateDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class KtorReviewsDataSource(
    private val httpClient: HttpClient
) : ReviewsDataSource {
    override suspend fun getReviews(clubSlug: String): Result<List<ReviewDto>, DataError.Remote> {
        return safeCall<List<ReviewDto>> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubSlug/list/reviews"
            )
        }
    }

    override suspend fun postReview(clubSlug: String, review: NewReviewItemDto): Result<Unit, DataError.Remote> {
        return safeCall {
            httpClient.post(
                urlString = "$BASE_URL/api/club/$clubSlug/list/reviews"
            ) {
                setBody(review)
            }
        }
    }

    override suspend fun deleteReview(clubSlug: String, reviewId: String): Result<Unit, DataError.Remote> {
        return safeCall {
            httpClient.delete(
                urlString = "$BASE_URL/api/club/$clubSlug/list/reviews/$reviewId"
            )
        }
    }

    override suspend fun submitScore(clubSlug: String, workId: String, score: Double): Result<Unit, DataError.Remote> {
        return safeCall {
            httpClient.post(
                urlString = "$BASE_URL/api/club/$clubSlug/reviews"
            ) {
                setBody(ScoreSubmissionDto(workId = workId, score = score))
            }
        }
    }

    override suspend fun updateScore(clubSlug: String, reviewId: String, score: Double): Result<Unit, DataError.Remote> {
        return safeCall {
            httpClient.put(
                urlString = "$BASE_URL/api/club/$clubSlug/reviews/$reviewId"
            ) {
                setBody(ScoreUpdateDto(score = score))
            }
        }
    }
}
