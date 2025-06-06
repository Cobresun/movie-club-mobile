package cobresun.movieclub.app.tmdb.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovieDto(
    val id: Int,
    val title: String,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("original_title")
    val originalTitle: String,
    val overview: String,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("media_type")
    val mediaType: String,
    val adult: Boolean,
    @SerialName("original_language")
    val originalLanguage: String?,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    val popularity: Double,
    @SerialName("release_date")
    val releaseDate: String,
    val video: Boolean,
    @SerialName("vote_count")
    val voteCount: Int
)
