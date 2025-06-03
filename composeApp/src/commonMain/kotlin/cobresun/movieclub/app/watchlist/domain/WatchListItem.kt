package cobresun.movieclub.app.watchlist.domain

import cobresun.movieclub.app.core.data.dto.TmdbExternalDataDto

data class WatchListItem(
    val id: String,
    val title: String,
    val createdDate: String,
    val externalId: String,
    val imageUrl: String,
    val externalDataDto: TmdbExternalDataDto?,
    val isNextMovie: Boolean,
)
