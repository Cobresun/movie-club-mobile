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
        val clubId: String
    ) : Route

    @Serializable
    data object EmptyClubs : Route

    @Serializable
    data object CreateClub : Route

    @Serializable
    data class ClubSettings(
        val clubId: String
    ) : Route
}
