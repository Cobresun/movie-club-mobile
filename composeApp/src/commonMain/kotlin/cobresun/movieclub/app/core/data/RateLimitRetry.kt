package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlinx.coroutines.delay

/**
 * Retries [block] with exponential backoff while it fails with
 * [DataError.Remote.TOO_MANY_REQUESTS] (HTTP 429), e.g. keyless Google Books quota.
 * Returns the last result once [maxAttempts] is reached.
 */
suspend fun <T> retryOnRateLimit(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 800,
    wait: suspend (Long) -> Unit = { delay(it) },
    block: suspend () -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote> {
    var backoff = initialDelayMs
    repeat(maxAttempts - 1) {
        val result = block()
        if (result !is Result.Error || result.error != DataError.Remote.TOO_MANY_REQUESTS) return result
        wait(backoff)
        backoff *= 2
    }
    return block()
}
