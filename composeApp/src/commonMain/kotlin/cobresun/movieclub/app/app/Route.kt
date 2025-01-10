package cobresun.movieclub.app.app

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data class ClubGraph(
        val clubId: String = COBRESUN_CLUB_ID,
    ) : Route

    @Serializable
    data class ReviewsList(
        val clubId: String
    ) : Route

    @Serializable
    data class WatchList(
        val clubId: String
    )
}

const val COBRESUN_CLUB_ID = "946516463182315521"
