package cobresun.movieclub.app.tmdb.data.network

import cobresun.movieclub.app.BuildKonfig.TMDB_API_KEY
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.tmdb.data.dto.TmdbMoviesDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"

class KtorTmdbDataSource(
    private val httpClient: HttpClient
) : TmdbDataSource {

    override suspend fun getTrendingMovies(): Result<TmdbMoviesDto, DataError.Remote> {
        return safeCall<TmdbMoviesDto> {
            httpClient.get("$TMDB_BASE_URL/trending/movie/week") {
                url {
                    parameters.append("api_key", TMDB_API_KEY)
                }
            }
        }
    }

    override suspend fun searchMovies(
        query: String,
        language: String,
        includeAdult: Boolean,
    ): Result<TmdbMoviesDto, DataError.Remote> {
        return safeCall<TmdbMoviesDto> {
            httpClient.get("$TMDB_BASE_URL/search/movie") {
                url {
                    parameters.append("api_key", TMDB_API_KEY)
                    parameters.append("query", query)
                    parameters.append("language", language)
                    parameters.append("include_adult", includeAdult.toString())
                }
            }
        }
    }
}
