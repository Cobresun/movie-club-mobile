package cobresun.movieclub.app.search.data.network

import cobresun.movieclub.app.BuildKonfig
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.search.data.dto.GoogleBooksSearchDto
import cobresun.movieclub.app.search.data.dto.TmdbMoviesDto
import cobresun.movieclub.app.search.data.dto.WorkRecommendationDto
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.request.get

interface SearchDataSource {
    /** A page of a TMDB movie collection: popular, now_playing, upcoming or top_rated. */
    suspend fun movieCollection(collection: String, page: Int): Result<TmdbMoviesDto, DataError.Remote>
    suspend fun searchMovies(query: String): Result<TmdbMoviesDto, DataError.Remote>
    suspend fun searchBooks(
        query: String,
        maxResults: Int = 20,
        extra: Map<String, String> = emptyMap(),
    ): Result<GoogleBooksSearchDto, DataError.Remote>
    suspend fun recommendations(clubSlug: String): Result<List<WorkRecommendationDto>, DataError.Remote>
}

private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
private const val GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1"

class KtorSearchDataSource(
    private val httpClient: HttpClient,
) : SearchDataSource {
    override suspend fun movieCollection(collection: String, page: Int): Result<TmdbMoviesDto, DataError.Remote> = safeCall {
        httpClient.get("$TMDB_BASE_URL/movie/$collection") {
            url {
                parameters.append("api_key", BuildKonfig.TMDB_API_KEY)
                parameters.append("language", "en-US")
                parameters.append("page", page.toString())
            }
        }
    }

    override suspend fun recommendations(clubSlug: String): Result<List<WorkRecommendationDto>, DataError.Remote> =
        safeCall { httpClient.get("$BASE_URL/api/club/$clubSlug/recommendations") }

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
        extra: Map<String, String>,
    ): Result<GoogleBooksSearchDto, DataError.Remote> = safeCall {
        httpClient.get("$GOOGLE_BOOKS_BASE_URL/volumes") {
            url {
                parameters.append("q", query)
                parameters.append("printType", "books")
                parameters.append("maxResults", maxResults.toString())
                extra.forEach { (key, value) -> parameters.append(key, value) }
                // Keyless requests work at low rate limits; an empty key param is a 400.
                BuildKonfig.GOOGLE_BOOKS_API_KEY.takeIf { it.isNotBlank() }?.let {
                    parameters.append("key", it)
                }
            }
        }
    }
}
