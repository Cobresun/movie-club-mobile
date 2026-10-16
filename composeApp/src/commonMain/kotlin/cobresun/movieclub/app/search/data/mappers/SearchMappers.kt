package cobresun.movieclub.app.search.data.mappers

import cobresun.movieclub.app.core.data.mappers.tmdbImageUrl
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.search.data.dto.GoogleBooksVolumeDto
import cobresun.movieclub.app.search.data.dto.TmdbMovieDto
import cobresun.movieclub.app.search.domain.WorkSearchResult

fun TmdbMovieDto.toSearchResult(): WorkSearchResult = WorkSearchResult(
    type = WorkType.MOVIE,
    externalId = id.toString(),
    title = title,
    subtitle = releaseDate?.take(4)?.takeIf { it.length == 4 },
    imageUrl = tmdbImageUrl(posterPath, "w342"),
)

fun GoogleBooksVolumeDto.toSearchResult(): WorkSearchResult {
    val info = volumeInfo
    val thumbnail = info?.imageLinks?.thumbnail ?: info?.imageLinks?.smallThumbnail
    return WorkSearchResult(
        type = WorkType.BOOK,
        externalId = id,
        title = info?.title ?: "Untitled",
        subtitle = listOfNotNull(info?.authors?.firstOrNull(), info?.publishedDate?.take(4))
            .joinToString(" · ")
            .ifBlank { null },
        imageUrl = thumbnail?.let(::secureGoogleImageUrl),
    )
}

/** Google returns http:// covers with a page-curl effect; normalise both. */
fun secureGoogleImageUrl(url: String): String =
    url.replaceFirst("http://", "https://")
        .replace("&edge=curl", "")
        .replace("edge=curl&", "")
