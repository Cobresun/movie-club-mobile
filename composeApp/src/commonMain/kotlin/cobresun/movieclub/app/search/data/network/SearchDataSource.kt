package cobresun.movieclub.app.search.data.network

import cobresun.movieclub.app.BuildKonfig
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.search.data.dto.GoogleBooksSearchDto
import cobresun.movieclub.app.search.data.dto.TmdbMoviesDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

interface SearchDataSource {
    suspend fun trendingMovies(): Result<TmdbMoviesDto, DataError.Remote>
    suspend fun searchMovies(query: String): Result<TmdbMoviesDto, DataError.Remote>
    suspend fun searchBooks(query: String, maxResults: Int = 20): Result<GoogleBooksSearchDto, DataError.Remote>
}

private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
private const val GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1"

class KtorSearchDataSource(
    private val httpClient: HttpClient,
) : SearchDataSource {
    override suspend fun trendingMovies(): Result<TmdbMoviesDto, DataError.Remote> = safeCall {
        httpClient.get("$TMDB_BASE_URL/trending/movie/week") {
            url { parameters.append("api_key", BuildKonfig.TMDB_API_KEY) }
        }
    }

    override suspend fun searchMovies(query: String): Result<TmdbMoviesDto, DataError.Remote> = safeCall {
        httpClient.get("$TMDB_BASE_URL/search/movie") {
            url {
                parameters.append("api_key", BuildKonfig.TMDB_API_KEY)
                parameters.append("query", query)
                parameters.append("language", "en-US")
                parameters.append("include_adult", "false")
            }
        }
    }

    override suspend fun searchBooks(
        query: String,
        maxResults: Int,
    ): Result<GoogleBooksSearchDto, DataError.Remote> = safeCall {
        httpClient.get("$GOOGLE_BOOKS_BASE_URL/volumes") {
            url {
                parameters.append("q", query)
                parameters.append("printType", "books")
                parameters.append("maxResults", maxResults.toString())
                // Keyless requests work at low rate limits; an empty key param is a 400.
                BuildKonfig.GOOGLE_BOOKS_API_KEY.takeIf { it.isNotBlank() }?.let {
                    parameters.append("key", it)
                }
            }
        }
    }
}
