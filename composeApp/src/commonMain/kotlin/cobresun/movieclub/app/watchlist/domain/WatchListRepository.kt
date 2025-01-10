package cobresun.movieclub.app.watchlist.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface WatchListRepository {
    suspend fun getWatchList(clubId: String): Result<List<WatchListItem>, DataError.Remote>
}
