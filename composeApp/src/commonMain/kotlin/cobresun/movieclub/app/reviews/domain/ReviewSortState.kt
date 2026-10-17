package cobresun.movieclub.app.reviews.domain

/**
 * The current sort. Picking an option always starts descending (newest or
 * highest first), like the web.
 */
data class ReviewSortState(
    val option: ReviewSortOption = ReviewSortOption.None,
    val descending: Boolean = true
) {
    /** True when any sort is active (not None). */
    val isActive: Boolean
        get() = option != ReviewSortOption.None

    /** Direction in words: dates read newest/oldest, ratings highest/lowest. Empty when inactive. */
    val directionLabel: String
        get() = when (option) {
            ReviewSortOption.None -> ""
            ReviewSortOption.DateReviewed -> if (descending) "Newest first" else "Oldest first"
            else -> if (descending) "Highest first" else "Lowest first"
        }
}
