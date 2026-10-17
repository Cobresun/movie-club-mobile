package cobresun.movieclub.app.reviews.domain

/** The ways reviews can be ordered (web `reviewSort.ts`: date, each member's rating, average). */
sealed interface ReviewSortOption {
    /** Server order: newest review first. */
    data object None : ReviewSortOption

    data object DateReviewed : ReviewSortOption

    data object AverageScore : ReviewSortOption

    data class MemberScore(val userId: String) : ReviewSortOption
}
