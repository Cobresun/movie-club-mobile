package cobresun.movieclub.app.reviews.domain

/** The ways reviews can be ordered. Each option carries its natural direction. */
sealed interface ReviewSortOption {
    val defaultDescending: Boolean

    /** Server order: newest review first. */
    data object None : ReviewSortOption {
        override val defaultDescending = true
    }

    data object DateReviewed : ReviewSortOption {
        override val defaultDescending = true
    }

    data object AverageScore : ReviewSortOption {
        override val defaultDescending = true
    }

    data object Title : ReviewSortOption {
        override val defaultDescending = false
    }

    data class MemberScore(val userId: String) : ReviewSortOption {
        override val defaultDescending = true
    }
}
