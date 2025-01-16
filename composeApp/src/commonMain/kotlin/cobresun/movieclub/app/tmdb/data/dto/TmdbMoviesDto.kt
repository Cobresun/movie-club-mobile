package cobresun.movieclub.app.tmdb.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMoviesDto(
    val page: Int,
    val results: List<TmdbMovieDto>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)
