package cobresun.movieclub.app.core.domain

/** True when [query] appears in the title or in the work's people/genre metadata. */
fun Work.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (title.contains(q, ignoreCase = true)) return true
    val haystack: List<String> = when (val d = details) {
        is WorkDetails.Movie -> d.directors + d.genres
        is WorkDetails.Book -> d.authors + d.subjects
        null -> emptyList()
    }
    return haystack.any { it.contains(q, ignoreCase = true) } || year?.toString() == q
}
