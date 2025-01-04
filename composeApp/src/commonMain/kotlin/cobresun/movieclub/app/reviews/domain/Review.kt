package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.reviews.data.dto.ScoreDto

data class Review(
    val id: String, // TODO: Value class might be smart here
    val title: String,
    val createdDate: String, // TODO: Convert to Date
    val imageUrl: String,
    val scores: Map<String, ScoreDto>,
)
