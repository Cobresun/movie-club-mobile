package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.Work

/**
 * A club's review as anyone with the share link sees it (web `SharedReviewView`):
 * every member with their score (null when they haven't scored) and the comments.
 */
data class SharedReview(
    val clubName: String,
    val work: Work,
    val members: List<SharedMemberScore>,
    val comments: List<ReviewComment>,
) {
    val scored: List<Double> get() = members.mapNotNull { it.score }

    /** Club average over the members who scored, or null when nobody has. */
    val average: Double? get() = scored.takeIf { it.isNotEmpty() }?.average()
}

data class SharedMemberScore(
    val userId: String,
    val name: String,
    val imageUrl: String?,
    val score: Double?,
    /** ISO-8601 instant the score was entered. */
    val scoredAt: String?,
)
