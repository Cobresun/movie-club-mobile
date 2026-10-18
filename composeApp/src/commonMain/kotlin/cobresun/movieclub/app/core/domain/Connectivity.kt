package cobresun.movieclub.app.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Whether the app can reach the network, and when it comes back. */
interface Connectivity {
    /** True while the device has a network with internet access. */
    val isOnline: StateFlow<Boolean>

    /**
     * Emits when the connection returns after being lost, once answers served from
     * storage meanwhile have been marked stale: screens reload quietly on it.
     */
    val reconnected: Flow<Unit>

    /**
     * True while screens show answers kept from earlier because the server couldn't be
     * reached (offline, or online with the server down or too slow).
     */
    val showingSavedData: StateFlow<Boolean>

    /** Marks in-memory answers stale and asks screens to reload, as on a reconnect. */
    fun retry()
}
