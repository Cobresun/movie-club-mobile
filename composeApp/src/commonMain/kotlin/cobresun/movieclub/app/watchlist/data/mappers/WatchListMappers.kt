package cobresun.movieclub.app.watchlist.data.mappers

import cobresun.movieclub.app.watchlist.data.dto.WatchListItemDto
import cobresun.movieclub.app.watchlist.domain.WatchListItem

fun WatchListItemDto.toWatchListItem(): WatchListItem {
    return WatchListItem(
        id = id
    )
}
