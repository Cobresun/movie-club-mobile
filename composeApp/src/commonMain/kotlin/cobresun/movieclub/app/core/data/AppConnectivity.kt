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
 * [onReconnect] (mark in-memory answers stale, replay queued writes), then tells screens.
 */
class AppConnectivity(
    private val monitor: NetworkMonitor,
    private val onReconnect: suspend () -> Unit,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : Connectivity {
    override val isOnline: StateFlow<Boolean> = monitor.isOnline

    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val reconnected: Flow<Unit> = _reconnected.asSharedFlow()

    init {
        scope.launch {
            var wasOnline = monitor.isOnline.value
            monitor.isOnline.collect { online ->
                if (online && !wasOnline) {
                    runCatching { onReconnect() }
                    _reconnected.tryEmit(Unit)
                }
                wasOnline = online
            }
        }
    }
}
