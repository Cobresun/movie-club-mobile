package cobresun.movieclub.app.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbExternalDataDto(
    val adult: Boolean,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    val budget: Int?,
    val homepage: String?,
    @SerialName("imdb_id")
    val imdbId: String?,
    @SerialName("original_language")
    val originalLanguage: String?,
    @SerialName("original_title")
    val originalTitle: String?,
    val overview: String?,
    val popularity: Double?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("release_date")
    val releaseDate: String?,
    val revenue: Int?,
    val runtime: Int?,
    val status: String?,
    val tagline: String?,
    @SerialName("vote_average")
    val voteAverage: Double?,
    val genres: List<String>?
)
