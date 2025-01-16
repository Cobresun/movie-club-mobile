package cobresun.movieclub.app.tmdb.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface TmdbRepository {
    suspend fun getTrendingMovies(): Result<List<TmdbMovie>, DataError.Remote>
    suspend fun searchMovies(query: String): Result<List<TmdbMovie>, DataError.Remote>
}
