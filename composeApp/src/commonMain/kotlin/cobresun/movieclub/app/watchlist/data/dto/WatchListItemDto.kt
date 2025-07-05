package cobresun.movieclub.app.watchlist.data.dto

import cobresun.movieclub.app.core.data.dto.TmdbExternalDataDto
import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WatchListItemDto(
    val id: String,
    val type: String,
    val title: String,
    @Serializable(with = InstantIso8601Serializer::class) val createdDate: Instant,
    val imageUrl: String,
    val externalId: String,
    @SerialName("externalData")
    val externalDataDto: TmdbExternalDataDto? = null
)
