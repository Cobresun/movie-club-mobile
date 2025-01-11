package cobresun.movieclub.app.watchlist.data.dto

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable
data class WatchListItemDto(
    val id: String,
    val title: String,
    @Serializable(with = InstantIso8601Serializer::class) val createdDate: Instant,
    val imageUrl: String,
    val externalId: String,
)
