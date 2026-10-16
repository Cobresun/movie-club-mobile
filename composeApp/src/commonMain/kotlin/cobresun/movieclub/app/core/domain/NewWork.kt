package cobresun.movieclub.app.core.domain

/** What is needed to add an external work (from search) to a club list. */
data class NewWork(
    val type: WorkType,
    val title: String,
    val externalId: String,
    val imageUrl: String?,
)
