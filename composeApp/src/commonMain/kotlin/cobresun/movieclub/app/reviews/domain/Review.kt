package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.Work
import kotlinx.datetime.LocalDate

/** A work on the club's reviews list with each member's score. */
data class Review(
    val work: Work,
    /** Scores keyed by member user id. Only current members are included. */
    val scores: Map<String, MemberScore>,
    /** Club average, or null when nobody has scored yet. */
    val average: Double?,
) {
    fun scoreOf(userId: String?): MemberScore? = userId?.let { scores[it] }

    fun withScores(workScores: WorkScores): Review = copy(scores = workScores.scores, average = workScores.average)

    /** Copy with [userId]'s score replaced (or removed when null) and the average recomputed. */
    fun withScore(userId: String, score: MemberScore?): Review {
        val updated = if (score == null) scores - userId else scores + (userId to score)
        return copy(
            scores = updated,
            average = updated.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
        )
    }
}

/** One work's scores keyed by member user id, plus the club average (null when nobody scored). */
data class WorkScores(val scores: Map<String, MemberScore>, val average: Double?)

data class MemberScore(
    val id: String,
    val userId: String,
    val value: Double,
    /** ISO-8601 instant the score was first entered, when known. */
    val createdAt: String? = null,
)

data class ReviewComment(
    val id: String,
    val workId: String,
    val userId: String,
    val userName: String,
    val userImageUrl: String?,
    val content: String,
    val createdDate: LocalDate?,
    val spoiler: Boolean,
    /** ISO-8601 instant posted, for "2 hours ago". */
    val createdAt: String? = null,
)
