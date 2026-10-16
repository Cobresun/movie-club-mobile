package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

class RateLimitRetryTest {
    /** Runs a suspend block that never actually suspends (the fake wait returns immediately). */
    private fun <T> run(block: suspend () -> T): T {
        var out: kotlin.Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { out = it })
        return out!!.getOrThrow()
    }

    @Test
    fun retriesWithBackoffUntilSuccess() {
        val waits = mutableListOf<Long>()
        var calls = 0
        val result = run {
            retryOnRateLimit(maxAttempts = 3, initialDelayMs = 100, wait = { waits += it }) {
                calls++
                if (calls < 3) Result.Error(DataError.Remote.TOO_MANY_REQUESTS) else Result.Success("ok")
            }
        }
        assertEquals(Result.Success("ok"), result)
        assertEquals(listOf(100L, 200L), waits)
    }

    @Test
    fun givesUpAfterMaxAttempts() {
        var calls = 0
        val result = run {
            retryOnRateLimit<String>(maxAttempts = 3, wait = {}) {
                calls++
                Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
            }
        }
        assertEquals(Result.Error(DataError.Remote.TOO_MANY_REQUESTS), result)
        assertEquals(3, calls)
    }

    @Test
    fun otherErrorsAreNotRetried() {
        var calls = 0
        run {
            retryOnRateLimit<String>(wait = {}) {
                calls++
                Result.Error(DataError.Remote.SERVER)
            }
        }
        assertEquals(1, calls)
    }
}
