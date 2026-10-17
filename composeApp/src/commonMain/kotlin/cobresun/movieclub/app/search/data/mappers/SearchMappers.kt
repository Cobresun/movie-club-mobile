package cobresun.movieclub.app.search.data.mappers

import cobresun.movieclub.app.core.data.mappers.tmdbImageUrl
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.search.data.dto.GoogleBooksVolumeDto
import cobresun.movieclub.app.search.data.dto.TmdbMovieDto
import cobresun.movieclub.app.search.domain.WorkSearchResult
import cobresun.movieclub.app.search.domain.WorkRecommendation
import cobresun.movieclub.app.search.data.dto.WorkRecommendationDto

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

/** Recommendations are TMDB movies; "Similar to A and B" is built from [WorkRecommendationDto.similarTo]. */
fun WorkRecommendationDto.toRecommendation(): WorkRecommendation = WorkRecommendation(
    work = WorkSearchResult(
        type = WorkType.MOVIE,
        externalId = externalId,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        reason = similarToReason(similarTo),
    ),
    similarTo = similarTo,
)

/** Web: `Similar to ${Intl.ListFormat("en", conjunction)}`, e.g. "Similar to A, B, and C". */
fun similarToReason(titles: List<String>): String? = when (titles.size) {
    0 -> null
    1 -> "Similar to ${titles[0]}"
    2 -> "Similar to ${titles[0]} and ${titles[1]}"
    else -> "Similar to ${titles.dropLast(1).joinToString(", ")}, and ${titles.last()}"
}
