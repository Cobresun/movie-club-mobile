package cobresun.movieclub.app.watchlist.domain

data class WatchListItem(
    val id: String,
    val title: String,
    val createdDate: String,
    val imageUrl: String,
    val externalId: String,
    val isNextMovie: Boolean
)
