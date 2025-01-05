package cobresun.movieclub.app.reviews.data.mappers

import cobresun.movieclub.app.reviews.data.dto.ReviewDto
import cobresun.movieclub.app.reviews.domain.Review
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun ReviewDto.toReview(): Review {
    return Review(
        id = id,
        title = title,
        createdDate = createdDate
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString(),
        imageUrl = imageUrl,
        scores = scores
    )
}
