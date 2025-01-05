package cobresun.movieclub.app.reviews.data.dto

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreDto(
    val id: String,
    val score: Double,

    @Serializable(with = InstantIso8601Serializer::class)
    @SerialName("created_date")
    val createdDate: Instant,
)
