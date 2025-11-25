@file:OptIn(kotlin.time.ExperimentalTime::class)
package cobresun.movieclub.app.reviews.data.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreDto(
    val id: String,
    val score: Double,

    @SerialName("created_date")
    val createdDate: Instant,
)
