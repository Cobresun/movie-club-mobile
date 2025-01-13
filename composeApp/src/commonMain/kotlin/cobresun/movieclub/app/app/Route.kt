package cobresun.movieclub.app.app

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data class ClubGraph(
        val clubId: String = COBRESUN_CLUB_ID,
    ) : Route
}

const val COBRESUN_CLUB_ID = "946516463182315521"
