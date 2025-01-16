package cobresun.movieclub.app.tmdb.data.network

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.tmdb.data.dto.TmdbMoviesDto

interface TmdbDataSource {
    suspend fun getTrendingMovies(): Result<TmdbMoviesDto, DataError.Remote>

    suspend fun searchMovies(
        query: String,
        language: String,
        includeAdult: Boolean,
    ): Result<TmdbMoviesDto, DataError.Remote>
}
