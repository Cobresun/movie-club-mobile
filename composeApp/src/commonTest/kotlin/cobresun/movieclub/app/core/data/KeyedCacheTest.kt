package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class KeyedCacheTest {
    private val time = TestTimeSource()
    private val cache = KeyedCache<String, Int>(60.seconds, time)
    private var calls = 0

    private suspend fun get(key: String = "club", refresh: Boolean = false, answer: Int = 1) =
        cache.get(key, refresh) { calls++; Result.Success(answer) }

    @Test
    fun concurrentReadsShareOneRequest() = runTest {
        val gate = CompletableDeferred<Unit>()
        val reads = List(5) {
            async { cache.get("club") { calls++; gate.await(); Result.Success(7) } }
        }
        repeat(5) { yield() }
        gate.complete(Unit)
        reads.forEach { assertEquals(Result.Success(7), it.await()) }
        assertEquals(1, calls)
    }

    @Test
    fun freshValueIsServedUntilItIsTooOld() = runTest {
        get()
        time += 59.seconds
        get()
        assertEquals(1, calls)
        time += 2.seconds
        get()
        assertEquals(2, calls)
    }

    @Test
    fun refreshAndInvalidateAskAgain() = runTest {
        get()
        get(refresh = true)
        assertEquals(2, calls)
        cache.invalidate("club")
        get()
        assertEquals(3, calls)
    }

    @Test
    fun keysAreSeparate() = runTest {
        get("a")
        get("b")
        get("a")
        assertEquals(2, calls)
    }

    @Test
    fun errorsAreNotKept() = runTest {
        cache.get("club") { calls++; Result.Error(DataError.Remote.NO_INTERNET) }
        assertEquals(Result.Success(1), get())
        assertEquals(2, calls)
    }

    @Test
    fun answerInFlightDuringAWriteIsNotKept() = runTest {
        val gate = CompletableDeferred<Unit>()
        val stale = async { cache.get("club") { gate.await(); Result.Success(1) } }
        repeat(5) { yield() }
        cache.invalidate("club")
        gate.complete(Unit)
        stale.await()
        // The pre-write answer was dropped, so this read goes to the network.
        assertEquals(Result.Success(2), get(answer = 2))
        assertEquals(1, calls)
    }

    @Test
    fun putServesTheWrittenValueAndClearForgetsIt() = runTest {
        cache.put("club", 9)
        assertEquals(Result.Success(9), get())
        assertEquals(9, cache.latest("club").value)
        cache.clear()
        assertEquals(Result.Success(1), get())
        assertEquals(1, calls)
    }
}
