package cobresun.movieclub.app.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A URL the app knows how to open. */
sealed interface DeepLink {
    data class JoinClub(val token: String) : DeepLink

    /**
     * A club page: `/club/<slug>[/<section>]`, or a shared review/list/statistics
     * link (`/share/club/<slug>/review/<workId>`). [reviewWorkId] opens that
     * review's details; a null [section] keeps whatever tab the club shows.
     * [wrappedYear] is the year of a `/club/<slug>/wrapped[/<year>]` link (0: latest).
     */
    data class Club(
        val slug: String,
        val section: ClubSection? = null,
        val reviewWorkId: String? = null,
        val wrappedYear: Int = 0,
    ) : DeepLink
}

enum class ClubSection { REVIEWS, LISTS, STATISTICS, AWARDS, SETTINGS, WRAPPED }

/**
 * Entry point for URLs handed to the app by the OS (Android intents, iOS `onOpenURL`).
 * Links are held until the UI consumes them, so a link opened while signed out is
 * acted on right after sign-in.
 */
object DeepLinks {
    private val hosts = setOf("movie-club.app", "www.movie-club.app")
    const val SCHEME = "movieclub"

    private val _pending = MutableStateFlow<DeepLink?>(null)
    val pending: StateFlow<DeepLink?> = _pending.asStateFlow()

    /** Returns true when [url] was recognised. */
    fun handle(url: String): Boolean {
        val link = parse(url) ?: return false
        _pending.value = link
        return true
    }

    fun consume(link: DeepLink) {
        _pending.compareAndSet(link, null)
    }

    fun parse(url: String): DeepLink? {
        val trimmed = url.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        val rest = trimmed.substring(schemeEnd + 3).substringBefore('?').substringBefore('#')
        val segments = rest.split('/').filter { it.isNotEmpty() }
        val path = when (scheme) {
            "https", "http" -> {
                val host = segments.firstOrNull()?.lowercase()?.substringBefore(':') ?: return null
                if (host !in hosts) return null
                segments.drop(1)
            }
            // movieclub://join-club/<token> (host is the first segment) or movieclub:///join-club/<token>
            SCHEME -> segments
            else -> return null
        }
        if (path.size == 2 && path[0] == "join-club" && path[1].isNotBlank()) {
            return DeepLink.JoinClub(path[1])
        }
        if (path.size >= 3 && path[0] == "share" && path[1] == "club") {
            val slug = path[2]
            return when (path.getOrNull(3)) {
                "review" -> path.getOrNull(4)?.let { DeepLink.Club(slug, ClubSection.REVIEWS, reviewWorkId = it) }
                "list" -> DeepLink.Club(slug, ClubSection.LISTS)
                "statistics" -> DeepLink.Club(slug, ClubSection.STATISTICS)
                else -> null
            }
        }
        if (path.size >= 2 && path[0] == "club") {
            // Web club sections; `watch-list` and `settings` are old links the web still redirects.
            val section = when (path.getOrNull(2)) {
                null -> null
                "reviews" -> ClubSection.REVIEWS
                "lists", "watch-list" -> ClubSection.LISTS
                "statistics" -> ClubSection.STATISTICS
                "awards" -> ClubSection.AWARDS
                // The invite page web shows after creating a club; settings has the invite link.
                "club", "settings", "invite" -> ClubSection.SETTINGS
                "wrapped" -> return DeepLink.Club(
                    path[1],
                    ClubSection.WRAPPED,
                    wrappedYear = path.getOrNull(3)?.takeIf { it.length == 4 }?.toIntOrNull() ?: 0,
                )
                else -> null
            }
            return DeepLink.Club(path[1], section)
        }
        return null
    }
}
