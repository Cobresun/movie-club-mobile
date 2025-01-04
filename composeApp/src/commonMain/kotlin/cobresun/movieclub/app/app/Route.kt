package cobresun.movieclub.app.app

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object ReviewsGraph : Route

    @Serializable
    data object ReviewsList : Route
}
