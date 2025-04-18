package cobresun.movieclub.app.watchlist.data.network

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.watchlist.data.dto.BacklogPostDto
import cobresun.movieclub.app.watchlist.data.dto.NextWorkDto
import cobresun.movieclub.app.watchlist.data.dto.PostWatchListItemDto
import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto

interface WatchListDataSource {
    suspend fun getNextWork(clubId: String): Result<NextWorkDto, DataError.Remote>

    suspend fun getWatchList(
        clubId: String
    ): Result<List<WatchListItemDto>, DataError.Remote>

    suspend fun postWatchList(
        clubId: String,
        postWatchListItemDto: PostWatchListItemDto
    ): Result<Unit, DataError.Remote>

    suspend fun deleteWatchList(
        clubId: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote>

    suspend fun getBacklog(
        clubId: String
    ): Result<List<WatchListItemDto>, DataError.Remote>

    suspend fun postBacklog(
        clubId: String,
        backlogPostDto: BacklogPostDto
    ): Result<Unit, DataError.Remote>

    suspend fun deleteBacklog(
        clubId: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote>
}
