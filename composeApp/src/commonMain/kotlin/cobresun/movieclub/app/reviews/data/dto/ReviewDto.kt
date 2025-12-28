@file:OptIn(kotlin.time.ExperimentalTime::class)
package cobresun.movieclub.app.reviews.data.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String,
    val title: String,
    val createdDate: Instant,
    val imageUrl: String,
    val scores: Map<String, ScoreDto>,
)
