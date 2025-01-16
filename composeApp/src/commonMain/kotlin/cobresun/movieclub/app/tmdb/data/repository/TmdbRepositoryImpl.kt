package cobresun.movieclub.app.tmdb.data.repository

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.tmdb.data.mappers.toTmdbMovie
import cobresun.movieclub.app.tmdb.data.network.TmdbDataSource
import cobresun.movieclub.app.tmdb.domain.TmdbRepository
import cobresun.movieclub.app.tmdb.domain.TmdbMovie

class TmdbRepositoryImpl(
    private val tmdbDataSource: TmdbDataSource
) : TmdbRepository {
    override suspend fun getTrendingMovies(): Result<List<TmdbMovie>, DataError.Remote> {
        return tmdbDataSource.getTrendingMovies()
            .map { tmdbMoviesDto ->
                tmdbMoviesDto.results
                    .map { it.toTmdbMovie() }
                    .sortedByDescending { it.popularity }
                    .subList(0, 10)
            }
    }

    override suspend fun searchMovies(query: String): Result<List<TmdbMovie>, DataError.Remote> {
        return tmdbDataSource.searchMovies(
            query = query,
            language = "en-US",
            includeAdult = false
        )
            .map { tmdbMoviesDto ->
                tmdbMoviesDto.results
                    .map { it.toTmdbMovie() }
                    .sortedByDescending { it.popularity }
                    .subList(0, 10)
            }
    }
}
