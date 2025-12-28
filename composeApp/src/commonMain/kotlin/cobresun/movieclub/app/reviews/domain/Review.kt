package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.User

data class Review(
    val id: String,
    val title: String,
    val createdDate: String,
    val imageUrl: String,
    val scores: Map<User, Score>,
)
