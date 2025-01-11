package cobresun.movieclub.app.watchlist.data.repository

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.watchlist.data.mappers.toWatchListItem
import cobresun.movieclub.app.watchlist.data.network.WatchListDataSource
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.domain.WatchListRepository

class WatchListRepositoryImpl(
    private val watchListDataSource: WatchListDataSource
) : WatchListRepository {
    override suspend fun getWatchList(clubId: String): Result<List<WatchListItem>, DataError.Remote> {
        return watchListDataSource.getWatchList(clubId)
            .map { watchListDtos ->
                watchListDtos.map { it.toWatchListItem() }
            }
    }

    override suspend fun getBacklog(clubId: String): Result<List<WatchListItem>, DataError.Remote> {
        return watchListDataSource.getBacklog(clubId)
            .map { watchListDtos ->
                watchListDtos.map { it.toWatchListItem() }
            }
    }
}
