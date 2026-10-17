package cobresun.movieclub.app.app

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object LandingPage : Route

    @Serializable
    data object AuthGraph : Route

    @Serializable
    data object ClubGraph: Route

    @Serializable
    data class Club(
        val clubSlug: String
    ) : Route

    @Serializable
    data object EmptyClubs : Route

    @Serializable
    data object CreateClub : Route

    /** The invite handoff right after creating a club; [clubType] is a `ClubType.value`. */
    @Serializable
    data class ClubInvite(
        val clubSlug: String,
        val clubName: String,
        val clubType: String,
    ) : Route

    @Serializable
    data class ClubSettings(
        val clubSlug: String
    ) : Route

    @Serializable
    data object Profile : Route

    /** A shared review link for a club the user isn't in (the public page). */
    @Serializable
    data class SharedReview(
        val clubSlug: String,
        val workId: String,
    ) : Route

    /** A club's year-in-review story. [year] 0 opens the latest released year. */
    @Serializable
    data class Wrapped(
        val clubSlug: String,
        val year: Int = 0,
    ) : Route

    /** [token] pre-fills the invite (e.g. from a join-club link). */
    @Serializable
    data class JoinClub(
        val token: String? = null
    ) : Route
}
