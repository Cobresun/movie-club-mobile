package cobresun.movieclub.app.search.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType

/**
 * A media-agnostic search hit, ready to be added to a club list.
 *
 * @param externalId the provider id (TMDB movie id or Google Books volume id)
 * @param subtitle short secondary line, e.g. release year or "Author · 1965"
 * @param reason why it's suggested, e.g. "Similar to Alien and Arrival"
 */
data class WorkSearchResult(
    val type: WorkType,
    val externalId: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val reason: String? = null,
)

/** A tab the add sheet offers for browsing the catalogue without searching (web `BrowseTab`). */
data class BrowseTab(val key: String, val label: String)

/** One page of a browse tab, and the number of the page after it if there is one. */
data class BrowsePage(val results: List<WorkSearchResult>, val nextPage: Int?)

/** A work recommended to a club, best first, with the reviewed works it grew from. */
data class WorkRecommendation(val work: WorkSearchResult, val similarTo: List<String>)

/** Web `clubTypeConfig(...).browseTabs`: TMDB collections for movies, Google Books subjects for books. */
fun browseTabs(type: WorkType): List<BrowseTab> = when (type) {
    WorkType.MOVIE -> listOf(
        BrowseTab("popular", "Popular"),
        BrowseTab("now_playing", "Now Playing"),
        BrowseTab("upcoming", "Upcoming"),
        BrowseTab("top_rated", "Top Rated"),
    )
    WorkType.BOOK -> listOf(
        BrowseTab("fiction", "Fiction"),
        BrowseTab("mystery", "Mystery"),
        BrowseTab("science fiction", "Sci-Fi"),
        BrowseTab("biography", "Biography"),
        BrowseTab("history", "History"),
    )
}

/** Only movie clubs have a source of similar works (web `supportsRecommendations`). */
val WorkType.supportsRecommendations: Boolean get() = this == WorkType.MOVIE

/**
 * Searches the external catalogue matching a club's media type: TMDB for
 * movies, Google Books for books.
 */
interface WorkSearchRepository {
    /** Free-text search. Returns an empty list for a blank query. */
    suspend fun search(type: WorkType, query: String): Result<List<WorkSearchResult>, DataError.Remote>

    /** One page (1-based) of a [browseTabs] tab. Book tabs are a single page. */
    suspend fun browse(type: WorkType, tabKey: String, page: Int): Result<BrowsePage, DataError.Remote>

    /** `GET /api/club/:slug/recommendations`: works similar to the ones the club scored well. */
    suspend fun recommendations(clubSlug: String): Result<List<WorkRecommendation>, DataError.Remote>
}
