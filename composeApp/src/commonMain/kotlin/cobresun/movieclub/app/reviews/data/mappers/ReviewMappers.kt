package cobresun.movieclub.app.reviews.data.mappers

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.data.mappers.parseLocalDate
import cobresun.movieclub.app.core.data.mappers.toWork
import cobresun.movieclub.app.reviews.data.dto.CommentDto
import cobresun.movieclub.app.reviews.data.dto.ReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.SharedReviewDto
import cobresun.movieclub.app.reviews.domain.SharedMemberScore
import cobresun.movieclub.app.reviews.domain.SharedReview
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import cobresun.movieclub.app.reviews.data.dto.ScoreDto
import cobresun.movieclub.app.reviews.domain.WorkScores
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewComment
import kotlinx.datetime.TimeZone
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.reviews.data.dto.MemberScoredWorkDto
import cobresun.movieclub.app.reviews.domain.ScoredWork

private const val AVERAGE_KEY = "average"

fun MemberScoredWorkDto.toScoredWork(): ScoredWork {
    // Reuse the list-item mapper for poster selection and metadata parsing.
    val work = WorkListItemDto(
        id = workId, type = type, title = title, createdDate = scoredDate,
        imageUrl = imageUrl, externalId = externalId, externalData = externalData,
    ).toWork()
    return ScoredWork(
        workId = workId,
        type = work.type,
        title = title,
        imageUrl = work.imageUrl,
        externalId = externalId,
        genres = (work.details as? WorkDetails.Movie)?.genres.orEmpty(),
        score = score,
        clubId = clubId,
        clubName = clubName,
        clubSlug = clubSlug,
        scoredDate = scoredDate,
    )
}

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

    val workScores = scores.toWorkScores()
    return Review(work = work, scores = workScores.scores, average = workScores.average)
}

/** A per-work score map (member ids plus a synthetic `"average"`) as domain scores. */
fun Map<String, ScoreDto>.toWorkScores(): WorkScores {
    val memberScores = filterKeys { it != AVERAGE_KEY }
        .mapValues { (userId, dto) -> MemberScore(id = dto.id, userId = userId, value = dto.score, createdAt = dto.createdDate) }
    val average = this[AVERAGE_KEY]?.score
        ?: memberScores.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average()
    return WorkScores(memberScores, average?.takeIf { memberScores.isNotEmpty() })
}

fun SharedReviewDto.toSharedReview(timeZone: TimeZone = TimeZone.currentSystemDefault()): SharedReview {
    val byUser = reviews.filter { it.userId != null }.associateBy { it.userId }
    return SharedReview(
        clubName = clubName,
        work = WorkListItemDto(
            id = work.id, type = work.type, title = work.title, createdDate = "",
            imageUrl = work.imageUrl, externalId = work.externalId, externalData = work.externalData,
        ).toWork(timeZone),
        members = members.map { m ->
            val row = byUser[m.id]
            SharedMemberScore(
                userId = m.id,
                name = m.name,
                imageUrl = m.image,
                score = (row?.score as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull(),
                scoredAt = row?.createdDate,
            )
        },
        comments = comments.map { it.toReviewComment(timeZone) },
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
        createdAt = createdDate,
    )
