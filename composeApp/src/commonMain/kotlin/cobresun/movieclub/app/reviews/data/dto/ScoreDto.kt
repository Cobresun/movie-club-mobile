package cobresun.movieclub.app.reviews.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScoreDto(
    val id: String,
    val score: Double,
)
