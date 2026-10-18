package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * [Connectivity] from the platform [NetworkMonitor]. When the network returns it first runs
 * [onReconnect] (mark in-memory answers stale) and [sendQueued] (writes kept while offline),
 * then tells screens. Writes queued in an earlier session are sent at launch when online.
 */
class AppConnectivity(
    private val monitor: NetworkMonitor,
    private val onReconnect: suspend () -> Unit,
    /** Sends writes queued offline; true when any reached the server. */
    private val sendQueued: suspend () -> Boolean = { false },
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : Connectivity {
    override val isOnline: StateFlow<Boolean> = monitor.isOnline

    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val reconnected: Flow<Unit> = _reconnected.asSharedFlow()

    init {
        scope.launch {
            var wasOnline: Boolean? = null
            monitor.isOnline.collect { online ->
                if (online && wasOnline == false) {
                    runCatching { onReconnect() }
                    runCatching { sendQueued() }
                    _reconnected.tryEmit(Unit)
                } else if (online && wasOnline == null) {
                    // Launch: only screens showing a queued write need to reload.
                    if (runCatching { sendQueued() }.getOrDefault(false)) _reconnected.tryEmit(Unit)
                }
                wasOnline = online
            }
        }
    }
}
