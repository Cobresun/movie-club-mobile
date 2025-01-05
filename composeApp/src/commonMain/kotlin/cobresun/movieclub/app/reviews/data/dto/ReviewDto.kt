package cobresun.movieclub.app.reviews.data.dto

import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String, // TODO: Value class might be smart here
    val title: String,
    @Serializable(with = InstantIso8601Serializer::class) val createdDate: Instant,
    val imageUrl: String,
    val scores: Map<String, ScoreDto>,
)
