package cobresun.movieclub.app.reviews.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String, // TODO: Value class might be smart here
    val title: String,
    val createdDate: String, // TODO: Convert to Date
    val imageUrl: String,
    val scores: Map<String, ScoreDto>,
)
