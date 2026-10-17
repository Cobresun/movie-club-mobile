package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.matchesQuery

/**
 * Orders [reviews] by [sortState]. Reviews missing the sorted value (e.g. a
 * member who hasn't scored) always sink to the end, whatever the direction.
 * Ties keep the incoming (newest-first) order.
 */
fun sortReviews(reviews: List<Review>, sortState: ReviewSortState): List<Review> {
    val selector: (Review) -> Comparable<*>? = when (val option = sortState.option) {
        ReviewSortOption.None -> return reviews
        ReviewSortOption.DateReviewed -> { r -> r.work.addedAt }
        ReviewSortOption.AverageScore -> { r -> r.average }
        is ReviewSortOption.MemberScore -> { r -> r.scores[option.userId]?.value }
    }
    val (present, missing) = reviews.partition { selector(it) != null }
    val comparator = compareBy(selector)
    val sorted = present.sortedWith(if (sortState.descending) comparator.reversed() else comparator)
    return sorted + missing
}

/**
 * Case-insensitive match on title, plus people/genre metadata so typing a
 * director, author or genre also finds works.
 */
fun filterReviews(reviews: List<Review>, query: String): List<Review> {
    val q = query.trim()
    if (q.isEmpty()) return reviews
    return reviews.filter { it.work.matchesQuery(q) }
}
