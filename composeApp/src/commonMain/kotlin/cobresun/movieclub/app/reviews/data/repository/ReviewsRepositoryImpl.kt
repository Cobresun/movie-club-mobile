package cobresun.movieclub.app.reviews.data.repository

import cobresun.movieclub.app.club.data.network.ClubDataSource
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.mappers.toNewReviewItemDto
import cobresun.movieclub.app.reviews.data.mappers.toReview
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.domain.NewReviewItem
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewsRepository

class ReviewsRepositoryImpl(
    private val clubDataSource: ClubDataSource,
    private val reviewsDataSource: ReviewsDataSource
) : ReviewsRepository {
    override suspend fun getReviews(clubSlug: String): Result<List<Review>, DataError.Remote> {
        val membersResult = clubDataSource.getMembers(clubSlug)
        val reviewsResult = reviewsDataSource.getReviews(clubSlug)

        return when (membersResult) {
            is Result.Success if reviewsResult is Result.Success -> {
                Result.Success(reviewsResult.data.map { it.toReview(membersResult.data) })
            }

            is Result.Error -> membersResult
            else -> reviewsResult as Result.Error
        }
    }

    override suspend fun postReview(
        clubSlug: String,
        review: NewReviewItem
    ): Result<Unit, DataError.Remote> {
        return reviewsDataSource.postReview(clubSlug, review.toNewReviewItemDto())
    }

    override suspend fun deleteReview(
        clubSlug: String,
        reviewId: String
    ): Result<Unit, DataError.Remote> {
        return reviewsDataSource.deleteReview(clubSlug, reviewId)
    }

    override suspend fun submitScore(
        clubSlug: String,
        reviewWorkId: String,
        scoreId: String?,
        score: Double
    ): Result<Unit, DataError.Remote> {
        return if (scoreId == null) {
            reviewsDataSource.submitScore(clubSlug, reviewWorkId, score)
        } else {
            reviewsDataSource.updateScore(clubSlug, scoreId, score)
        }
    }
}
