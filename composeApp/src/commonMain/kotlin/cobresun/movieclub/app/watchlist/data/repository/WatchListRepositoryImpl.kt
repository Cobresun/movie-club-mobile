package cobresun.movieclub.app.watchlist.data.repository

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.watchlist.data.dto.BacklogPostDto
import cobresun.movieclub.app.watchlist.data.dto.PostWatchListItemDto
import cobresun.movieclub.app.watchlist.data.mappers.toWatchListItem
import cobresun.movieclub.app.watchlist.data.network.WatchListDataSource
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.domain.WatchListRepository

class WatchListRepositoryImpl(
    private val watchListDataSource: WatchListDataSource
) : WatchListRepository {
    override suspend fun getWatchList(clubId: String): Result<List<WatchListItem>, DataError.Remote> {
        val nextWork = watchListDataSource.getNextWork(clubId)
        val nextWorkId = if (nextWork is Result.Success) nextWork.data.workId else null

        return watchListDataSource.getWatchList(clubId)
            .map { watchListDtos ->
                watchListDtos
                    .map {
                        val isNext = if (nextWork is Result.Success) it.id == nextWorkId else false
                        it.toWatchListItem(isNextMovie = isNext)
                    }
                    .sortedBy { !it.isNextMovie }
            }
    }

    override suspend fun postWatchList(
        clubId: String,
        watchListItem: WatchListItem
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.postWatchList(
            clubId = clubId,
            postWatchListItemDto = PostWatchListItemDto(
                id = watchListItem.id,
                type = watchListItem.type.value,
                title = watchListItem.title,
                externalId = watchListItem.externalId,
                imageUrl = watchListItem.imageUrl,
                createdDate = watchListItem.createdDate,
                externalData = watchListItem.externalDataDto
            )
        )
    }

    override suspend fun deleteWatchList(
        clubId: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.deleteWatchList(clubId, watchListItemId)
    }

    override suspend fun getBacklog(clubId: String): Result<List<WatchListItem>, DataError.Remote> {
        return watchListDataSource.getBacklog(clubId)
            .map { watchListDtos ->
                watchListDtos.map { it.toWatchListItem() }
            }
    }

    override suspend fun postBacklog(
        clubId: String,
        tmdbMovie: TmdbMovie
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.postBacklog(
            clubId = clubId,
            backlogPostDto = BacklogPostDto(
                type = WorkType.MOVIE.value,
                title = tmdbMovie.title,
                externalId = tmdbMovie.id.toString(),
                imageUrl = tmdbMovie.imageUrl
            )
        )
    }

    override suspend fun deleteBacklog(
        clubId: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.deleteBacklog(clubId, watchListItemId)
    }
}
