package cobresun.movieclub.app.tmdb.data.mappers

import cobresun.movieclub.app.tmdb.data.dto.TmdbMovieDto
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import kotlinx.datetime.LocalDate

const val BASE_IMAGE_URL = "https://image.tmdb.org/t/p/w154/"

fun TmdbMovieDto.toTmdbMovie(): TmdbMovie {
    return TmdbMovie(
        id = id,
        title = title,
        releaseYear = LocalDate.parse(releaseDate).year.toString(),
        popularity = popularity,
        imageUrl = "${BASE_IMAGE_URL}${posterPath}"
    )
}
