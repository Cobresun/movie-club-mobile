package cobresun.movieclub.app.reviews.data.mappers

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.data.mappers.parseLocalDate
import cobresun.movieclub.app.core.data.mappers.toWork
import cobresun.movieclub.app.reviews.data.dto.CommentDto
import cobresun.movieclub.app.reviews.data.dto.ReviewItemDto
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewComment
import kotlinx.datetime.TimeZone

private const val AVERAGE_KEY = "average"

fun ReviewItemDto.toReview(timeZone: TimeZone = TimeZone.currentSystemDefault()): Review {
    val work = WorkListItemDto(
        id = id,
        type = type,
        title = title,
        createdDate = createdDate,
        imageUrl = imageUrl,
        externalId = externalId,
        externalData = externalData,
    ).toWork(timeZone)

    val memberScores = scores
        .filterKeys { it != AVERAGE_KEY }
        .mapValues { (userId, dto) -> MemberScore(id = dto.id, userId = userId, value = dto.score) }

    val average = scores[AVERAGE_KEY]?.score
        ?: memberScores.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average()

    return Review(
        work = work,
        scores = memberScores,
        average = average?.takeIf { memberScores.isNotEmpty() },
    )
}

fun CommentDto.toReviewComment(timeZone: TimeZone = TimeZone.currentSystemDefault()): ReviewComment =
    ReviewComment(
        id = id,
        workId = workId,
        userId = userId,
        userName = userName,
        userImageUrl = userImage,
        content = content,
        createdDate = parseLocalDate(createdDate, timeZone),
        spoiler = spoiler,
    )
