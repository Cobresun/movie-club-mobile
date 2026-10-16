package cobresun.movieclub.app.reviews.domain

/**
 * Mirrors web's hide-others'-scores rule (`reviewScores.ts`): a member's view of
 * a reviewed work hides everyone else's scores and the club average until
 * they've scored it themselves or explicitly revealed it. Their own score is
 * always visible. Like web, manual reveals live only as long as the reviews
 * screen (web keeps them in an in-memory set on the reviews view, not storage).
 */
object ScoreReveal {
    /** Scored it yourself, or revealed it by hand. Nobody signed in → nothing scored. */
    fun isRevealed(review: Review, currentUserId: String?, revealedWorkIds: Set<String>): Boolean =
        review.scoreOf(currentUserId) != null || review.work.id in revealedWorkIds

    /**
     * A score that isn't the reader's own: another member's, or the average
     * (pass a null [memberId]) that would give theirs away.
     */
    fun isOthersScore(memberId: String?, currentUserId: String?): Boolean =
        currentUserId == null || memberId != currentUserId

    fun isHidden(memberId: String?, currentUserId: String?, revealed: Boolean): Boolean =
        !revealed && isOthersScore(memberId, currentUserId)

    /** Whether to offer "Reveal club scores": still hidden and something to hide. */
    fun canReveal(review: Review, currentUserId: String?, revealedWorkIds: Set<String>): Boolean =
        !isRevealed(review, currentUserId, revealedWorkIds) &&
            (review.average != null || review.scores.keys.any { isOthersScore(it, currentUserId) })
}
