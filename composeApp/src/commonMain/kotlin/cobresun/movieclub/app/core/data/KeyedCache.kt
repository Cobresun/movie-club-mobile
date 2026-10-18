package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * In-memory, per-key copy of a remote read, so screens that open together share one request.
 *
 * - A value younger than [maxAge] is served without a request.
 * - Concurrent reads of the same key join the one request in flight (shared [Deferred]).
 * - `refresh = true` skips the stored value (pull-to-refresh) but still joins a request
 *   that started after the last [invalidate].
 * - [invalidate] after a write: requests already in flight can't store their (older) answer.
 * - Errors are never stored; the next read asks again.
 *
 * Requests run in the cache's own scope, so one caller being cancelled (a screen closing)
 * doesn't cancel the request for the others waiting on it.
 */
class KeyedCache<K, V>(
    private val maxAge: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private class Entry<V> {
        val latest = MutableStateFlow<V?>(null)
        var hasValue = false
        var fetchedAt: TimeMark? = null
        var generation = 0
        var inFlight: Deferred<Result<V, DataError.Remote>>? = null
    }

    private val mutex = Mutex()
    private val entries = mutableMapOf<K, Entry<V>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun get(
        key: K,
        refresh: Boolean = false,
        fetch: suspend () -> Result<V, DataError.Remote>,
    ): Result<V, DataError.Remote> {
        val request = mutex.withLock {
            val entry = entries.getOrPut(key) { Entry() }
            val fetchedAt = entry.fetchedAt
            if (!refresh && entry.hasValue && fetchedAt != null && fetchedAt.elapsedNow() < maxAge) {
                @Suppress("UNCHECKED_CAST")
                return Result.Success(entry.latest.value as V)
            }
            entry.inFlight ?: start(entry, fetch).also { entry.inFlight = it }
        }
        return request.await()
    }

    private fun start(
        entry: Entry<V>,
        fetch: suspend () -> Result<V, DataError.Remote>,
    ): Deferred<Result<V, DataError.Remote>> {
        val generation = entry.generation
        return scope.async {
            val result = fetch()
            mutex.withLock {
                // Superseded by a write or a sign-out while this was in flight: drop it.
                if (entry.generation == generation) {
                    if (result is Result.Success) store(entry, result.data)
                    entry.inFlight = null
                }
            }
            result
        }
    }

    private fun store(entry: Entry<V>, value: V) {
        entry.latest.value = value
        entry.hasValue = true
        entry.fetchedAt = timeSource.markNow()
    }

    /** The latest stored value for [key] (null until one arrives). */
    suspend fun latest(key: K): StateFlow<V?> =
        mutex.withLock { entries.getOrPut(key) { Entry() }.latest.asStateFlow() }

    /** Stores [value] as the fresh answer for [key], e.g. what a write returned. */
    suspend fun put(key: K, value: V) = mutex.withLock {
        val entry = entries.getOrPut(key) { Entry() }
        entry.generation++
        entry.inFlight = null
        store(entry, value)
    }

    /** The next read of [key] goes to the network. Observers keep the last value meanwhile. */
    suspend fun invalidate(key: K) = mutex.withLock { entries[key]?.let(::expire) }

    /** [invalidate] for every key. */
    suspend fun invalidateAll() = mutex.withLock { entries.values.forEach(::expire) }

    /** Forgets everything, including values observers hold (sign-out). */
    suspend fun clear() = mutex.withLock {
        entries.values.forEach { entry ->
            expire(entry)
            entry.hasValue = false
            entry.latest.value = null
        }
        entries.clear()
    }

    private fun expire(entry: Entry<V>) {
        entry.generation++
        entry.inFlight = null
        entry.fetchedAt = null
    }
}
