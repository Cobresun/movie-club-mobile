package cobresun.movieclub.app.stats.data

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.stats.domain.CreditedPerson
import cobresun.movieclub.app.stats.domain.StatsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

/** One entry of `GET /api/club/:slug/reviews/cast`. */
@Serializable
data class CastCreditDto(
    val name: String,
    val character: String? = null,
    val profilePath: String? = null,
)

private const val TMDB_PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w185"

fun Map<String, List<CastCreditDto>>.toCreditedCast(): Map<String, List<CreditedPerson>> =
    mapValues { (_, cast) ->
        cast.map { dto ->
            CreditedPerson(
                name = dto.name,
                imageUrl = dto.profilePath?.takeIf { it.isNotBlank() }?.let { TMDB_PROFILE_BASE_URL + it },
            )
        }
    }

class StatsRepositoryImpl(
    private val httpClient: HttpClient,
) : StatsRepository {
    override suspend fun getReviewedCast(clubSlug: String): Result<Map<String, List<CreditedPerson>>, DataError.Remote> =
        safeCall<Map<String, List<CastCreditDto>>> { httpClient.get("$BASE_URL/api/club/$clubSlug/reviews/cast") }
            .map { it.toCreditedCast() }
}
