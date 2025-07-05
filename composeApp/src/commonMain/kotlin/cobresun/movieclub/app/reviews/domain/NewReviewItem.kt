package cobresun.movieclub.app.reviews.domain

data class NewReviewItem(
    val type: String,
    val title: String,
    val externalId: String,
    val imageUrl: String
)
