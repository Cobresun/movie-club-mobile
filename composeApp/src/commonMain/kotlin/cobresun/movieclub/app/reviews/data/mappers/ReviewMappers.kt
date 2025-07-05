package cobresun.movieclub.app.reviews.data.mappers

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.reviews.data.dto.NewReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ReviewDto
import cobresun.movieclub.app.reviews.data.dto.ScoreDto
import cobresun.movieclub.app.reviews.domain.NewReviewItem
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.Score
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun ReviewDto.toReview(memberDtos: List<MemberDto>): Review {
    return Review(
        id = id,
        title = title,
        createdDate = createdDate
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString(),
        imageUrl = imageUrl,
        scores = scores.toScores(memberDtos)
    )
}

fun Map<String, ScoreDto>.toScores(memberDtos: List<MemberDto>): Map<User, Score> {
    return this
        .filter { (memberId, _) -> memberId != "average" && memberDtos.any { it.id == memberId } }
        .map { (memberId, scoreDto) ->
            val memberDto = requireNotNull(memberDtos.find { it.id == memberId })

            val user = User(
                id = memberDto.id,
                name = memberDto.name,
                imageUrl = memberDto.image
            )

            val score = Score(
                id = scoreDto.id,
                value = scoreDto.score
            )

            user to score
        }.toMap()
}

fun NewReviewItem.toNewReviewItemDto(): NewReviewItemDto {
    return NewReviewItemDto(
        type = type,
        title = title,
        externalId = externalId,
        imageUrl = imageUrl
    )
}
