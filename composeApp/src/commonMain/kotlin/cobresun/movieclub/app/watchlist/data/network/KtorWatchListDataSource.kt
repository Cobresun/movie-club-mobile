package cobresun.movieclub.app.watchlist.data.network

import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.watchlist.data.dto.BacklogPostDto
import cobresun.movieclub.app.watchlist.data.dto.NextWorkDto
import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class KtorWatchListDataSource(
    private val httpClient: HttpClient
) : WatchListDataSource {
    override suspend fun getNextWork(clubId: String): Result<NextWorkDto, DataError.Remote> {
        return safeCall<NextWorkDto> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubId/nextWork"
            )
        }
    }

    override suspend fun getWatchList(clubId: String): Result<List<WatchListItemDto>, DataError.Remote> {
        return safeCall<List<WatchListItemDto>> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubId/list/watchlist"
            )
        }
    }

    override suspend fun postWatchList(clubId: String): Result<Unit, DataError.Remote> {
        TODO("Not yet implemented")
    }

    override suspend fun getBacklog(clubId: String): Result<List<WatchListItemDto>, DataError.Remote> {
        return safeCall<List<WatchListItemDto>> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubId/list/backlog"
            )
        }
    }

    override suspend fun postBacklog(clubId: String, backlogPostDto: BacklogPostDto): Result<Unit, DataError.Remote> {
        return safeCall {
            httpClient.post(
                urlString = "$BASE_URL/api/club/$clubId/list/backlog"
            ) {
                setBody(backlogPostDto)
            }
        }
    }
}
