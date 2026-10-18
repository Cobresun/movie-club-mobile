package cobresun.movieclub.app.core.presentation

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import kotlinx.datetime.LocalDate
import cobresun.movieclub.app.core.domain.formatRuntime

internal val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Oct 16, 2026" */
fun LocalDate.formatShort(): String = "${MONTHS[month.ordinal]} $day, $year"

/** Compact secondary line under a poster: "2010 · Christopher Nolan" or "1965 · Frank Herbert". */
fun Work.posterSubtitle(): String? {
    val person = when (val d = details) {
        is WorkDetails.Movie -> d.directors.firstOrNull()
        is WorkDetails.Book -> d.authors.firstOrNull()
        null -> null
    }
    return listOfNotNull(year?.toString(), person).joinToString(" · ").ifBlank { null }
}

/** Facts row in detail sheets: year, runtime/pages, genres. */
fun Work.metaFacts(includeTmdbRating: Boolean = true): List<String> = when (val d = details) {
    is WorkDetails.Movie -> listOfNotNull(
        year?.toString(),
        d.runtimeMinutes?.let(::formatRuntime),
        d.voteAverage?.takeIf { it > 0 && includeTmdbRating }?.let { "TMDB " + (kotlin.math.round(it * 10) / 10).toString() },
    )

    is WorkDetails.Book -> listOfNotNull(
        year?.toString(),
        d.pageCount?.let { "$it pages" },
    )

    null -> listOfNotNull(year?.toString())
}

/** Singular noun for UI copy. */
val WorkType.noun: String
    get() = when (this) {
        WorkType.MOVIE -> "movie"
        WorkType.BOOK -> "book"
    }

val WorkType.finishedVerb: String
    get() = when (this) {
        WorkType.MOVIE -> "watched"
        WorkType.BOOK -> "read"
    }
