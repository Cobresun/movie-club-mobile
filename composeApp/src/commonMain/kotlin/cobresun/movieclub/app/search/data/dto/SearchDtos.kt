package cobresun.movieclub.app.search.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMoviesDto(
    val results: List<TmdbMovieDto> = emptyList(),
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)

/** One entry of `GET /api/club/:slug/recommendations`. */
@Serializable
data class WorkRecommendationDto(
    val externalId: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val similarTo: List<String> = emptyList(),
)

@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String = "",
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val popularity: Double? = null,
)

@Serializable
data class GoogleBooksSearchDto(
    val items: List<GoogleBooksVolumeDto>? = null,
)

@Serializable
data class GoogleBooksVolumeDto(
    val id: String,
    val volumeInfo: GoogleBooksVolumeInfoDto? = null,
)

@Serializable
data class GoogleBooksVolumeInfoDto(
    val title: String? = null,
    val authors: List<String>? = null,
    val publishedDate: String? = null,
    val ratingsCount: Int? = null,
    val imageLinks: GoogleBooksImageLinksDto? = null,
)

@Serializable
data class GoogleBooksImageLinksDto(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
)
