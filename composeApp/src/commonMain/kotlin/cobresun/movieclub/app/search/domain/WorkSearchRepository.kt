package cobresun.movieclub.app.search.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType

/**
 * A media-agnostic search hit, ready to be added to a club list.
 *
 * @param externalId the provider id (TMDB movie id or Google Books volume id)
 * @param subtitle short secondary line, e.g. release year or "Author · 1965"
 */
data class WorkSearchResult(
    val type: WorkType,
    val externalId: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
)

/**
 * Searches the external catalogue matching a club's media type: TMDB for
 * movies, Google Books for books.
 */
interface WorkSearchRepository {
    /** Free-text search. Returns an empty list for a blank query. */
    suspend fun search(type: WorkType, query: String): Result<List<WorkSearchResult>, DataError.Remote>

    /** Suggestions shown before the user types (trending movies / popular books). */
    suspend fun suggestions(type: WorkType): Result<List<WorkSearchResult>, DataError.Remote>
}
