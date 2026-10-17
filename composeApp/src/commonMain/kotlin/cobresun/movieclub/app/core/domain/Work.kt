package cobresun.movieclub.app.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A movie or book that belongs to a club, as it appears on one of the club's
 * lists (a user list or the reviews list).
 *
 * @param addedAt ISO-8601 instant the work was added to its list; sorts lexicographically.
 * @param imageUrl best available poster/cover URL (fully qualified).
 */
data class Work(
    val id: String,
    val type: WorkType,
    val title: String,
    val addedAt: String,
    val addedDate: LocalDate?,
    val imageUrl: String?,
    val externalId: String?,
    val addedByUserId: String?,
    val details: WorkDetails?,
) {
    /**
     * The UTC calendar year the work was added (reviewed), matching the web and
     * server, which bucket by the ISO instant's year rather than the device's
     * time zone (a New Year's Eve review in the Americas is still last year's).
     */
    val addedYearUtc: Int?
        get() = runCatching { Instant.parse(addedAt).toLocalDateTime(TimeZone.UTC).year }.getOrNull()

    /** Release year for movies, first publish year for books. */
    val year: Int?
        get() = when (details) {
            is WorkDetails.Movie -> details.releaseDate?.take(4)?.toIntOrNull()
            is WorkDetails.Book -> details.firstPublishYear
            null -> null
        }
}

/** Provider metadata for a work. Discriminated by media kind. */
sealed interface WorkDetails {
    data class Movie(
        val overview: String?,
        val tagline: String?,
        val releaseDate: String?,
        val runtimeMinutes: Int?,
        val genres: List<String>,
        val directors: List<String>,
        val castNames: List<String>,
        val voteAverage: Double?,
        val backdropUrl: String?,
        /** Full cast with characters; only present from the per-work details endpoint. */
        val cast: List<CastMember> = emptyList(),
        /** Server-filtered major cast (top-billed or popular), used for "Familiar face" facts. */
        val majorCastNames: List<String> = emptyList(),
        /** IMDb title id ("tt0078748"), for the IMDb link. */
        val imdbId: String? = null,
        /** Director name -> TMDB profile photo, for those who have one (Wrapped, stats leaderboards). */
        val directorImageUrls: Map<String, String> = emptyMap(),
    ) : WorkDetails

    data class Book(
        val description: String?,
        val authors: List<String>,
        val subjects: List<String>,
        val firstPublishYear: Int?,
        val pageCount: Int?,
        val coverUrl: String?,
    ) : WorkDetails
}

data class CastMember(
    val name: String,
    val character: String?,
    val profileImageUrl: String?,
)
