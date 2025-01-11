package cobresun.movieclub.app.watchlist.data.network

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class KtorWatchListDataSource(
    private val httpClient: HttpClient
) : WatchListDataSource {
    override suspend fun getWatchList(clubId: String): Result<List<WatchListItemDto>, DataError.Remote> {
        return safeCall<List<WatchListItemDto>> {
            httpClient.get(
                urlString = "$BASE_URL/club/$clubId/list/watchlist"
            )
        }
    }

    override suspend fun getBacklog(clubId: String): Result<List<WatchListItemDto>, DataError.Remote> {
        return safeCall<List<WatchListItemDto>> {
            httpClient.get(
                urlString = "$BASE_URL/club/$clubId/list/backlog"
            )
        }
    }
}
