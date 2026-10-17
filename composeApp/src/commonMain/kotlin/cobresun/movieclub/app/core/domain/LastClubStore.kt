package cobresun.movieclub.app.core.domain

/** Remembers which club the user last opened, so the app reopens there (like the web app). */
interface LastClubStore {
    suspend fun get(): String?
    suspend fun set(slug: String)
}

/**
 * The club to open on launch: the remembered one if the user is still a member,
 * otherwise the first club.
 */
fun defaultClubSlug(clubs: List<Club>, lastSlug: String?): String? =
    clubs.firstOrNull { it.slug == lastSlug }?.slug ?: clubs.firstOrNull()?.slug
