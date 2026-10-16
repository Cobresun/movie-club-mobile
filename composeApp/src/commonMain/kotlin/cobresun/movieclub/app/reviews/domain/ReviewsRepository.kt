package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ReviewsRepository {
    suspend fun getReviews(clubSlug: String): Result<List<Review>, DataError.Remote>
    suspend fun postReview(clubSlug: String, review: NewReviewItem): Result<Unit, DataError.Remote>
    suspend fun deleteReview(clubSlug: String, reviewId: String): Result<Unit, DataError.Remote>
    suspend fun submitScore(
        clubSlug: String,
        reviewWorkId: String,
        scoreId: String?,
        score: Double
    ): Result<Unit, DataError.Remote>
}
