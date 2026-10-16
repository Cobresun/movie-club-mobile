package cobresun.movieclub.app.providers.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

/** A subscription streaming service a movie is available on. */
data class WatchProvider(
    val id: Int,
    val name: String,
    val logoUrl: String?,
)

/**
 * Where a movie streams in one region (TMDB data, powered by JustWatch).
 * @param link JustWatch page for the movie in that region.
 */
data class WatchAvailability(
    val providers: List<WatchProvider>,
    val link: String?,
)

/** Looks up streaming availability for movies. */
interface WatchProvidersRepository {
    /**
     * Subscription ("flatrate") services for TMDB movie [tmdbId] in [region] (ISO 3166-1, e.g. "US"),
     * ordered by display priority. Null when nothing is available there; never falls back to
     * another region.
     */
    suspend fun getProviders(tmdbId: String, region: String): Result<WatchAvailability?, DataError.Remote>
}
