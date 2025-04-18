package cobresun.movieclub.app.watchlist.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BacklogPostDto(
    val type: String,
    val title: String,
    val externalId: String,
    val imageUrl: String?,
)
