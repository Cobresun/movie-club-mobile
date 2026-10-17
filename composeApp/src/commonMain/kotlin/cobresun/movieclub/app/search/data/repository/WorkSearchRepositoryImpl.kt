package cobresun.movieclub.app.search.data.repository

import cobresun.movieclub.app.core.data.retryOnRateLimit
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.search.data.dto.GoogleBooksVolumeDto
import cobresun.movieclub.app.search.data.mappers.toSearchResult
import cobresun.movieclub.app.search.data.network.SearchDataSource
import cobresun.movieclub.app.search.domain.WorkSearchRepository
import cobresun.movieclub.app.search.domain.WorkSearchResult
import cobresun.movieclub.app.search.domain.BrowsePage
import cobresun.movieclub.app.search.domain.WorkRecommendation
import cobresun.movieclub.app.search.data.mappers.toRecommendation

class WorkSearchRepositoryImpl(
    private val dataSource: SearchDataSource,
) : WorkSearchRepository {

    override suspend fun search(
        type: WorkType,
        query: String,
    ): Result<List<WorkSearchResult>, DataError.Remote> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.Success(emptyList())
        return when (type) {
            WorkType.MOVIE -> dataSource.searchMovies(trimmed).map { dto ->
                dto.results.map { it.toSearchResult() }
            }

            WorkType.BOOK -> retryOnRateLimit { dataSource.searchBooks(trimmed) }.map { dto ->
                dto.items.orEmpty().sortedByPopularity().map { it.toSearchResult() }
            }
        }
    }

    override suspend fun browse(type: WorkType, tabKey: String, page: Int): Result<BrowsePage, DataError.Remote> =
        when (type) {
            WorkType.MOVIE -> dataSource.movieCollection(tabKey, page).map { dto ->
                BrowsePage(
                    results = dto.results.map { it.toSearchResult() },
                    nextPage = if (dto.page < dto.totalPages) dto.page + 1 else null,
                )
            }

            // Google Books has no trending endpoint: each tab is the subject's newest
            // volumes, one page. Coverless volumes make the grid look broken, so drop them.
            WorkType.BOOK -> retryOnRateLimit {
                dataSource.searchBooks(
                    query = "subject:\"$tabKey\"",
                    maxResults = 24,
                    extra = mapOf("orderBy" to "newest", "langRestrict" to "en"),
                )
            }.map { dto ->
                BrowsePage(
                    results = dto.items.orEmpty().map { it.toSearchResult() }.filter { it.imageUrl != null },
                    nextPage = null,
                )
            }
        }

    override suspend fun recommendations(clubSlug: String): Result<List<WorkRecommendation>, DataError.Remote> =
        dataSource.recommendations(clubSlug).map { list -> list.map { it.toRecommendation() } }

    /** Google's relevance order surfaces obscure editions; nudge well-rated ones up (stable). */
    private fun List<GoogleBooksVolumeDto>.sortedByPopularity(): List<GoogleBooksVolumeDto> =
        sortedByDescending { it.volumeInfo?.ratingsCount ?: 0 }
}
