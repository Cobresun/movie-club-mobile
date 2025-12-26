package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.User

/**
 * Represents the different ways reviews can be sorted.
 * Each option includes its default sort direction.
 */
sealed interface ReviewSortOption {
    val defaultDescending: Boolean

    /**
     * No sorting applied - reviews in original API order
     */
    data object None : ReviewSortOption {
        override val defaultDescending = true
    }

    /**
     * Sort by date the review was created
     */
    data object DateReviewed : ReviewSortOption {
        override val defaultDescending = true // Newest first
    }

    /**
     * Sort by average score across all members
     */
    data object AverageScore : ReviewSortOption {
        override val defaultDescending = true // Highest first
    }

    /**
     * Sort by a specific member's score
     * @param user The member whose scores to sort by
     */
    data class MemberScore(val user: User) : ReviewSortOption {
        override val defaultDescending = true // Highest first
    }
}
