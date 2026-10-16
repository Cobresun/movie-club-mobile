package cobresun.movieclub.app.watchlist.data.network

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.watchlist.data.dto.BacklogPostDto
import cobresun.movieclub.app.watchlist.data.dto.NextWorkDto
import cobresun.movieclub.app.watchlist.data.dto.PostWatchListItemDto
import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto

interface WatchListDataSource {
    suspend fun getNextWork(clubSlug: String): Result<NextWorkDto, DataError.Remote>

    suspend fun setNextWork(clubSlug: String, workId: String): Result<Unit, DataError.Remote>

    suspend fun getWatchList(
        clubSlug: String
    ): Result<List<WatchListItemDto>, DataError.Remote>

    suspend fun postWatchList(
        clubSlug: String,
        postWatchListItemDto: PostWatchListItemDto
    ): Result<Unit, DataError.Remote>

    suspend fun deleteWatchList(
        clubSlug: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote>

    suspend fun getBacklog(
        clubSlug: String
    ): Result<List<WatchListItemDto>, DataError.Remote>

    suspend fun postBacklog(
        clubSlug: String,
        backlogPostDto: BacklogPostDto
    ): Result<Unit, DataError.Remote>

    suspend fun deleteBacklog(
        clubSlug: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote>
}
