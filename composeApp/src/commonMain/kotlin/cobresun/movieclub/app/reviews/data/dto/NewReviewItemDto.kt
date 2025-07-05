package cobresun.movieclub.app.reviews.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewReviewItemDto(
    val type: String,
    val title: String,
    val externalId: String,
    val imageUrl: String
)
