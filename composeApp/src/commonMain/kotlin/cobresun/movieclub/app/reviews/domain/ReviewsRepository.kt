package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ReviewsRepository {
    suspend fun getReviews(clubId: String): Result<List<Review>, DataError.Remote>
    suspend fun postReview(clubId: String, review: NewReviewItem): Result<Unit, DataError.Remote>
}
