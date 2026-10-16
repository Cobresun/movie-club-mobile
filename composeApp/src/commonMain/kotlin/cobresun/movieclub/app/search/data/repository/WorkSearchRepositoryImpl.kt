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

    override suspend fun suggestions(type: WorkType): Result<List<WorkSearchResult>, DataError.Remote> =
        when (type) {
            WorkType.MOVIE -> dataSource.trendingMovies().map { dto ->
                dto.results.map { it.toSearchResult() }
            }

            WorkType.BOOK -> retryOnRateLimit { dataSource.searchBooks("subject:fiction", maxResults = 20) }.map { dto ->
                dto.items.orEmpty().sortedByPopularity().map { it.toSearchResult() }
            }
        }

    /** Google's relevance order surfaces obscure editions; nudge well-rated ones up (stable). */
    private fun List<GoogleBooksVolumeDto>.sortedByPopularity(): List<GoogleBooksVolumeDto> =
        sortedByDescending { it.volumeInfo?.ratingsCount ?: 0 }
}
