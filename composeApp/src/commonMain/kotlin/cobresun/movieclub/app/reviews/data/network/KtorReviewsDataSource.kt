package cobresun.movieclub.app.reviews.data.network

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.data.dto.ReviewDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class KtorReviewsDataSource(
    private val httpClient: HttpClient
) : ReviewsDataSource {
    override suspend fun getReviews(clubId: String): Result<List<ReviewDto>, DataError.Remote> {
        return safeCall<List<ReviewDto>> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubId/list/reviews"
            )
        }
    }
}
