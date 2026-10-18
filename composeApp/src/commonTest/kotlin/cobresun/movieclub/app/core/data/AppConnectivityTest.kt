package cobresun.movieclub.app.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppConnectivityTest {
    private class FakeMonitor(online: Boolean) : NetworkMonitor {
        val state = MutableStateFlow(online)
        override val isOnline: StateFlow<Boolean> = state
    }

    @Test
    fun reconnectRunsHookBeforeTellingScreens() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeMonitor(online = false)
        val events = mutableListOf<String>()
        val connectivity = AppConnectivity(monitor, onReconnect = { events += "stale" }, scope = backgroundScope)
        backgroundScope.launch { connectivity.reconnected.collect { events += "reload" } }

        monitor.state.value = true

        assertEquals(listOf("stale", "reload"), events)
    }

    @Test
    fun onlyAnOfflineToOnlineChangeCounts() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeMonitor(online = true)
        var reconnects = 0
        val connectivity = AppConnectivity(monitor, onReconnect = { reconnects++ }, scope = backgroundScope)
        backgroundScope.launch { connectivity.reconnected.collect { } }

        monitor.state.value = true // no change: still online
        monitor.state.value = false
        monitor.state.value = true
        monitor.state.value = false
        monitor.state.value = true

        assertEquals(2, reconnects)
    }

    @Test
    fun retryRunsHookAndReloadsWhileOnline() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeMonitor(online = true)
        val events = mutableListOf<String>()
        val connectivity = AppConnectivity(monitor, onReconnect = { events += "stale" }, scope = backgroundScope)
        backgroundScope.launch { connectivity.reconnected.collect { events += "reload" } }

        connectivity.retry()

        assertEquals(listOf("stale", "reload"), events)
    }
}
