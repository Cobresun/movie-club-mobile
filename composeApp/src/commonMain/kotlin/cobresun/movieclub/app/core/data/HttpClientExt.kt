package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.auth.data.dto.BetterAuthErrorDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive

/**
 * Runs the request and decodes its body off the main thread: callers are
 * usually in viewModelScope (Main), and Ktor decodes on the caller's context,
 * so big payloads would otherwise block frames.
 */
suspend inline fun <reified T> safeCall(
    crossinline execute: suspend () -> HttpResponse
): Result<T, DataError.Remote> = withContext(Dispatchers.Default) { safeCallHere<T> { execute() } }

/** [safeCall] on the current dispatcher. */
suspend inline fun <reified T> safeCallHere(
    execute: () -> HttpResponse
): Result<T, DataError.Remote> {
    val response = try {
        execute()
    } catch (e: Exception) {
        // Rethrow if *our* coroutine was cancelled; Ktor's request timeout is also a
        // CancellationException but leaves the caller active, so it's classified below.
        currentCoroutineContext().ensureActive()
        return Result.Error(classifyNetworkException(e))
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError.Remote> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Result.Error(DataError.Remote.SERIALIZATION)
            }
        }

        403 -> {
            // Better Auth returns 403 for email not verified
            Result.Error(DataError.Remote.EMAIL_NOT_CONFIRMED)
        }

        408 -> Result.Error(DataError.Remote.REQUEST_TIMEOUT)
        429 -> Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Remote.SERVER)
        else -> {
            // Try to parse Better Auth error response for specific error cases
            try {
                val errorResponse = response.body<BetterAuthErrorDto>()
                if (errorResponse.message?.contains("email", ignoreCase = true) == true &&
                    errorResponse.message.contains("verif", ignoreCase = true)) {
                    Result.Error(DataError.Remote.EMAIL_NOT_CONFIRMED)
                } else {
                    Result.Error(DataError.Remote.UNKNOWN)
                }
            } catch (e: Exception) {
                Result.Error(DataError.Remote.UNKNOWN)
            }
        }
    }
}
