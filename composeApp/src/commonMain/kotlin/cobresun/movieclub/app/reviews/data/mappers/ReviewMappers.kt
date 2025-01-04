package cobresun.movieclub.app.reviews.data.mappers

import cobresun.movieclub.app.reviews.data.dto.ReviewDto
import cobresun.movieclub.app.reviews.domain.Review

fun ReviewDto.toReview(): Review {
    return Review(
        id = id,
        title = title,
        createdDate = createdDate,
        imageUrl = imageUrl,
        scores = scores
    )
}
