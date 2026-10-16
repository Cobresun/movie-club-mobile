package cobresun.movieclub.app.watchlist.data.repository

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.domain.flatMap
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
    override suspend fun getWatchList(clubSlug: String): Result<List<WatchListItem>, DataError.Remote> {
        return watchListDataSource.getWatchList(clubSlug).flatMap { watchListDtos ->
            watchListDataSource.getNextWork(clubSlug).map { nextWorkDto ->
                val items = watchListDtos.map { it.toWatchListItem() }
                val nextId = nextWorkDto.workId
                if (nextId != null) {
                    items.map { it.copy(isNextMovie = it.id == nextId) }
                        .sortedBy { !it.isNextMovie }
                } else {
                    items
                }
            }
        }
    }

    override suspend fun postWatchList(
        clubSlug: String,
        watchListItem: WatchListItem
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.postWatchList(
            clubSlug = clubSlug,
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

    override suspend fun postWatchListFromMovie(
        clubSlug: String,
        tmdbMovie: TmdbMovie
    ): Result<Unit, DataError.Remote> {
        // Note: The API doesn't support adding directly to watchlist from a movie.
        // Items must go through backlog first where the server generates an ID.
        // This method adds to backlog as a workaround.
        return watchListDataSource.postBacklog(
            clubSlug = clubSlug,
            backlogPostDto = BacklogPostDto(
                type = WorkType.MOVIE.value,
                title = tmdbMovie.title,
                externalId = tmdbMovie.id.toString(),
                imageUrl = tmdbMovie.imageUrl
            )
        )
    }

    override suspend fun deleteWatchList(
        clubSlug: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.deleteWatchList(clubSlug, watchListItemId)
    }

    override suspend fun getBacklog(clubSlug: String): Result<List<WatchListItem>, DataError.Remote> {
        return watchListDataSource.getBacklog(clubSlug)
            .map { dtos -> dtos.map { it.toWatchListItem() } }
    }

    override suspend fun postBacklog(
        clubSlug: String,
        tmdbMovie: TmdbMovie
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.postBacklog(
            clubSlug = clubSlug,
            backlogPostDto = BacklogPostDto(
                type = WorkType.MOVIE.value,
                title = tmdbMovie.title,
                externalId = tmdbMovie.id.toString(),
                imageUrl = tmdbMovie.imageUrl
            )
        )
    }

    override suspend fun deleteBacklog(
        clubSlug: String,
        watchListItemId: String
    ): Result<Unit, DataError.Remote> {
        return watchListDataSource.deleteBacklog(clubSlug, watchListItemId)
    }

    override suspend fun setNextWatch(clubSlug: String, workId: String): Result<Unit, DataError.Remote> {
        return watchListDataSource.setNextWork(clubSlug, workId)
    }
}
