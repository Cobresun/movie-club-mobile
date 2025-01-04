package cobresun.movieclub.app.reviews.data.repository

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.reviews.data.mappers.toReview
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewsRepository

class ReviewsRepositoryImpl(
    private val reviewsDataSource: ReviewsDataSource
) : ReviewsRepository {
    override suspend fun getReviews(clubId: String): Result<List<Review>, DataError.Remote> {
        return reviewsDataSource.getReviews(clubId)
            .map { reviewDtos ->
                reviewDtos.map { it.toReview() }
            }
    }
}
