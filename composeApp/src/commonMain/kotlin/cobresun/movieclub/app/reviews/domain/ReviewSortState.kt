package cobresun.movieclub.app.reviews.domain

/**
 * Represents the current sort state for reviews
 */
data class ReviewSortState(
    val option: ReviewSortOption = ReviewSortOption.None,
    val descending: Boolean = true
) {
    /**
     * Returns true if any sorting is active (not None)
     */
    val isActive: Boolean
        get() = option != ReviewSortOption.None
}
