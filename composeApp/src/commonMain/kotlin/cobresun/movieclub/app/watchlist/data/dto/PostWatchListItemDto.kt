package cobresun.movieclub.app.watchlist.data.dto

import cobresun.movieclub.app.core.data.dto.TmdbExternalDataDto
import kotlinx.serialization.Serializable

@Serializable
data class PostWatchListItemDto(
    val id: String,
    val title: String,
    val externalId: String,
    val imageUrl: String,
    val createdDate: String,
    val externalData: TmdbExternalDataDto?
)
