package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.platform.AppLog
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private class ServerErrorDto(val error: String? = null, val message: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Extracts `{error}` / `{message}` from a server error body; falls back to short plain text. */
fun parseServerMessage(body: String): String? {
    if (body.isBlank()) return null
    return try {
        val dto = errorJson.decodeFromString(ServerErrorDto.serializer(), body)
        dto.error ?: dto.message
    } catch (e: Exception) {
        body.trim().takeIf { it.length < 200 && !it.startsWith("<") && !it.startsWith("{") }
    }
}

fun statusToRemote(status: Int): DataError.Remote = when (status) {
    403 -> DataError.Remote.EMAIL_NOT_CONFIRMED // Better Auth: email not verified; also "forbidden"
    408 -> DataError.Remote.REQUEST_TIMEOUT
    429 -> DataError.Remote.TOO_MANY_REQUESTS
    in 500..599 -> DataError.Remote.SERVER
    else -> DataError.Remote.UNKNOWN
}

/**
 * Like [safeCall], but keeps the server's error message and status, and tolerates
 * non-JSON success bodies when [T] is [Unit].
 */
suspend inline fun <reified T> apiCall(crossinline execute: suspend () -> HttpResponse): Result<T, ApiError> =
    // Off the main thread, like safeCall: Ktor decodes on the caller's context.
    withContext(Dispatchers.Default) { apiCallHere<T> { execute() } }

suspend inline fun <reified T> apiCallHere(execute: () -> HttpResponse): Result<T, ApiError> {
    val response = try {
        execute()
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        return Result.Error(ApiError(classifyNetworkException(e).also { logFailure(it, e) }))
    }
    val status = response.status.value
    if (status in 200..299) {
        if (T::class == Unit::class) return Result.Success(Unit as T)
        return try {
            Result.Success(response.body<T>())
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logFailure(DataError.Remote.SERIALIZATION, e, response)
            Result.Error(ApiError(DataError.Remote.SERIALIZATION, httpCode = status))
        }
    }
    val message = try {
        parseServerMessage(response.bodyAsText())
    } catch (e: Exception) {
        null
    }
    val remote = statusToRemote(status)
    logFailure(remote, null, response)
    return Result.Error(ApiError(remote, message, status))
}

fun <T> Result<T, ApiError>.toRemote(): Result<T, DataError.Remote> = when (this) {
    is Result.Success -> this
    is Result.Error -> Result.Error(error.remote)
}

/**
 * Reports failures worth looking into (the server erroring, or answering in a shape we
 * can't read) as warnings for error monitoring; expected ones (offline, 4xx) stay debug lines.
 */
fun logFailure(error: DataError.Remote, cause: Throwable?, response: HttpResponse? = null) {
    val where = response?.let { "${it.request.method.value} ${it.request.url.encodedPath} → ${it.status.value}" }
    val text = listOfNotNull(error.name, where).joinToString(": ")
    val unexpected = error == DataError.Remote.SERVER || error == DataError.Remote.SERIALIZATION ||
        (error == DataError.Remote.UNKNOWN && response == null)
    if (unexpected) AppLog.w(NETWORK_TAG, text, cause)
    else AppLog.d(NETWORK_TAG) { if (cause != null) "$text ($cause)" else text }
}

const val NETWORK_TAG = "MovieClubHttp"
