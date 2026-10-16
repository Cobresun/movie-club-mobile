package cobresun.movieclub.app.core.domain

/** API routes address a club by [slug]; [id] is only for identity comparisons. */
data class Club(
    val id: String,
    val slug: String,
    val name: String,
    val type: ClubType = ClubType.MOVIE,
)

enum class ClubType(val value: String) {
    MOVIE("movie"),
    BOOK("book");

    companion object {
        fun from(value: String?): ClubType = entries.firstOrNull { it.value == value } ?: MOVIE
    }
}
