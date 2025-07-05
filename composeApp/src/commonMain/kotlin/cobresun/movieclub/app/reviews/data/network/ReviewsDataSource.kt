package cobresun.movieclub.app.reviews.data.network

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.dto.NewReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ReviewDto

interface ReviewsDataSource {
    suspend fun getReviews(clubId: String): Result<List<ReviewDto>, DataError.Remote>
    suspend fun postReview(clubId: String, review: NewReviewItemDto): Result<Unit, DataError.Remote>
}
