package cobresun.movieclub.app.tmdb.data.mappers

import cobresun.movieclub.app.tmdb.data.dto.TmdbMovieDto
import cobresun.movieclub.app.tmdb.domain.TmdbMovie

fun TmdbMovieDto.toTmdbMovie(): TmdbMovie {
    return TmdbMovie(
        id = id,
        title = title,
        releaseYear = releaseDate.subSequence(0, 4).toString(), // TODO: Sure feels like there needs to be a better way than this...
        popularity = popularity,
    )
}
