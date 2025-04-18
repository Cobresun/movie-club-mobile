package cobresun.movieclub.app.watchlist.data.mappers

import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun WatchListItemDto.toWatchListItem(isNextMovie: Boolean = false): WatchListItem {
    return WatchListItem(
        id = id,
        type = WorkType.MOVIE,
        title = title,
        createdDate = createdDate
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString(),
        externalId = externalId,
        imageUrl = imageUrl,
        externalDataDto = externalDataDto,
        isNextMovie = isNextMovie,
    )
}
