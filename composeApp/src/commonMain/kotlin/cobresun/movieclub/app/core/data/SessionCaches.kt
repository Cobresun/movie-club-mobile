package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The signed-in session's shared copies of club reads (one [KeyedCache] each), so the
 * tabs of a club opening together send each request once. Repository implementations
 * read through these and invalidate them after their writes; keys are club slugs.
 * Cleared on sign-in and sign-out.
 */
class SessionCaches {
    // Rarely changed by anyone else: kept a few minutes.
    val clubs = KeyedCache<Unit, List<Club>>(5.minutes)
    val me = KeyedCache<Unit, Member>(5.minutes)
    val members = KeyedCache<String, List<Member>>(5.minutes)
    val features = KeyedCache<String, ClubFeatures>(5.minutes)

    // Other members change these all the time: a tab shown a minute later asks again.
    val reviews = KeyedCache<String, List<Review>>(60.seconds)
    val upNext = KeyedCache<String, String?>(60.seconds)
    val allItems = KeyedCache<String, List<ListedWork>>(60.seconds)

    /** Id of a club's hidden reviews list; it never changes. */
    val reviewsListId = KeyedCache<String, String>(Duration.INFINITE)

    /** Everything one club's lists or reviews feed: after a write that may touch any of it. */
    suspend fun invalidateClubWorks(slug: String) {
        reviews.invalidate(slug)
        upNext.invalidate(slug)
        allItems.invalidate(slug)
    }

    /** The club is gone or renamed for this user: forget everything kept under [slug]. */
    suspend fun forgetClub(slug: String) {
        clubs.invalidate(Unit)
        members.invalidate(slug)
        features.invalidate(slug)
        invalidateClubWorks(slug)
    }

    suspend fun clear() {
        clubs.clear()
        me.clear()
        members.clear()
        features.clear()
        reviews.clear()
        upNext.clear()
        allItems.clear()
        reviewsListId.clear()
    }
}
