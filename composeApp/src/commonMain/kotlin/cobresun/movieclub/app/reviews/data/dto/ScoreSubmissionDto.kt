package cobresun.movieclub.app.reviews.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScoreSubmissionDto(
    val workId: String,
    val score: Double
)

@Serializable
data class ScoreUpdateDto(
    val score: Double
)
