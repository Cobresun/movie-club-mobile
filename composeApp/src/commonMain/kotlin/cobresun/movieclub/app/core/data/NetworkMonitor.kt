package cobresun.movieclub.app.core.data

import kotlinx.coroutines.flow.StateFlow

/** The platform's view of the default network (Android ConnectivityManager, iOS NWPathMonitor). */
interface NetworkMonitor {
    /** True while a network with internet access is up. */
    val isOnline: StateFlow<Boolean>
}
