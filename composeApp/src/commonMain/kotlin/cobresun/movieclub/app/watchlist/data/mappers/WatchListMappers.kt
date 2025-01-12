package cobresun.movieclub.app.watchlist.data.mappers

import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun WatchListItemDto.toWatchListItem(isNextMovie: Boolean = false): WatchListItem {
    return WatchListItem(
        id = id,
        title = title,
        createdDate = createdDate
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString(),
        imageUrl = imageUrl,
        externalId = externalId,
        isNextMovie = isNextMovie
    )
}
