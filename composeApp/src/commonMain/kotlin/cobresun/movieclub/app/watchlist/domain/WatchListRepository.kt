package cobresun.movieclub.app.watchlist.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.tmdb.domain.TmdbMovie

interface WatchListRepository {
    suspend fun getWatchList(clubId: String): Result<List<WatchListItem>, DataError.Remote>
    suspend fun postWatchList(clubId: String, watchListItem: WatchListItem): Result<Unit, DataError.Remote>
    suspend fun postWatchListFromMovie(clubId: String, tmdbMovie: TmdbMovie): Result<Unit, DataError.Remote>
    suspend fun deleteWatchList(clubId: String, watchListItemId: String): Result<Unit, DataError.Remote>

    suspend fun getBacklog(clubId: String): Result<List<WatchListItem>, DataError.Remote>
    suspend fun postBacklog(clubId: String, tmdbMovie: TmdbMovie): Result<Unit, DataError.Remote>
    suspend fun deleteBacklog(clubId: String, watchListItemId: String): Result<Unit, DataError.Remote>
}
