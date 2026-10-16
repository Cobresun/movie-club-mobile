package cobresun.movieclub.app.core.domain

enum class WorkType(val value: String) {
    MOVIE("movie"),
    BOOK("book");

    companion object {
        fun from(value: String?): WorkType = entries.firstOrNull { it.value == value } ?: MOVIE
    }
}

/** A club's works are always of the type matching the club (1:1). */
val ClubType.workType: WorkType
    get() = when (this) {
        ClubType.MOVIE -> WorkType.MOVIE
        ClubType.BOOK -> WorkType.BOOK
    }
