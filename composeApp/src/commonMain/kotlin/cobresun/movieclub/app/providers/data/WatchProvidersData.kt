package cobresun.movieclub.app.providers.data

import cobresun.movieclub.app.BuildKonfig
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.providers.domain.WatchAvailability
import cobresun.movieclub.app.providers.domain.WatchProvider
import cobresun.movieclub.app.providers.domain.WatchProvidersRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchProvidersResponseDto(
    val results: Map<String, WatchProvidersRegionDto> = emptyMap(),
)

@Serializable
data class WatchProvidersRegionDto(
    val link: String? = null,
    val flatrate: List<WatchProviderDto> = emptyList(),
)

@Serializable
data class WatchProviderDto(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = Int.MAX_VALUE,
)

/** Subscription services only (matches web WatchProviders.vue); null when the region has none. */
fun WatchProvidersResponseDto.toAvailability(region: String): WatchAvailability? {
    val regionData = results[region.uppercase()] ?: return null
    val providers = regionData.flatrate
        .sortedBy { it.displayPriority }
        .map { WatchProvider(it.providerId, it.providerName, it.logoPath?.let { p -> "https://image.tmdb.org/t/p/w92$p" }) }
    if (providers.isEmpty()) return null
    return WatchAvailability(providers, regionData.link)
}

class TmdbWatchProvidersRepository(
    private val httpClient: HttpClient,
) : WatchProvidersRepository {
    private val cache = mutableMapOf<String, WatchProvidersResponseDto>()

    override suspend fun getProviders(tmdbId: String, region: String): Result<WatchAvailability?, DataError.Remote> {
        cache[tmdbId]?.let { return Result.Success(it.toAvailability(region)) }
        return safeCall<WatchProvidersResponseDto> {
            httpClient.get("https://api.themoviedb.org/3/movie/$tmdbId/watch/providers") {
                url { parameters.append("api_key", BuildKonfig.TMDB_API_KEY) }
            }
        }.map { dto ->
            cache[tmdbId] = dto
            dto.toAvailability(region)
        }
    }
}
