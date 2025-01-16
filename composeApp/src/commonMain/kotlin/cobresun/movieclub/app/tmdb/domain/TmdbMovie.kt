package cobresun.movieclub.app.tmdb.domain

data class TmdbMovie(
    val id: Int,
    val title: String,
    val releaseYear: String,
    val popularity: Double,
)
