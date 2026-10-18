package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
        return Result.Error(classifyNetworkException(e).also { logFailure(it, e) })
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError.Remote> {
    val status = response.status.value
    if (status in 200..299) {
        return try {
            Result.Success(response.body<T>())
        } catch (e: NoTransformationFoundException) {
            logFailure(DataError.Remote.SERIALIZATION, e, response)
            Result.Error(DataError.Remote.SERIALIZATION)
        }
    }
    var remote = statusToRemote(status)
    if (remote == DataError.Remote.UNKNOWN) {
        // Better Auth can also say "email not verified" in a 4xx body.
        val message = runCatching { parseServerMessage(response.bodyAsText()) }.getOrNull()
        if (message != null && message.contains("email", ignoreCase = true) && message.contains("verif", ignoreCase = true)) {
            remote = DataError.Remote.EMAIL_NOT_CONFIRMED
        }
    }
    logFailure(remote, null, response)
    return Result.Error(remote)
}
