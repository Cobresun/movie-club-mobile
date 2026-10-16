package cobresun.movieclub.app.reviews.data.network

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.dto.NewReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ReviewDto

interface ReviewsDataSource {
    suspend fun getReviews(clubSlug: String): Result<List<ReviewDto>, DataError.Remote>
    suspend fun postReview(clubSlug: String, review: NewReviewItemDto): Result<Unit, DataError.Remote>
    suspend fun deleteReview(clubSlug: String, reviewId: String): Result<Unit, DataError.Remote>
    suspend fun submitScore(clubSlug: String, workId: String, score: Double): Result<Unit, DataError.Remote>
    suspend fun updateScore(clubSlug: String, reviewId: String, score: Double): Result<Unit, DataError.Remote>
}
