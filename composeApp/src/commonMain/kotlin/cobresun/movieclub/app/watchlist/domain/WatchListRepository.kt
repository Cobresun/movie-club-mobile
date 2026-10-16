package cobresun.movieclub.app.watchlist.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.tmdb.domain.TmdbMovie

interface WatchListRepository {
    suspend fun getWatchList(clubSlug: String): Result<List<WatchListItem>, DataError.Remote>
    suspend fun postWatchList(clubSlug: String, watchListItem: WatchListItem): Result<Unit, DataError.Remote>
    suspend fun postWatchListFromMovie(clubSlug: String, tmdbMovie: TmdbMovie): Result<Unit, DataError.Remote>
    suspend fun deleteWatchList(clubSlug: String, watchListItemId: String): Result<Unit, DataError.Remote>

    suspend fun getBacklog(clubSlug: String): Result<List<WatchListItem>, DataError.Remote>
    suspend fun postBacklog(clubSlug: String, tmdbMovie: TmdbMovie): Result<Unit, DataError.Remote>
    suspend fun deleteBacklog(clubSlug: String, watchListItemId: String): Result<Unit, DataError.Remote>

    suspend fun setNextWatch(clubSlug: String, workId: String): Result<Unit, DataError.Remote>
}
