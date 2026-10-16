package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.ServerErrorDto
import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json

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
    403 -> DataError.Remote.EMAIL_NOT_CONFIRMED
    408 -> DataError.Remote.REQUEST_TIMEOUT
    429 -> DataError.Remote.TOO_MANY_REQUESTS
    in 500..599 -> DataError.Remote.SERVER
    else -> DataError.Remote.UNKNOWN
}

/**
 * Like core `safeCall`, but keeps the server's error message and tolerates
 * non-JSON success bodies when [T] is [Unit].
 */
suspend inline fun <reified T> apiCall(execute: () -> HttpResponse): Result<T, ApiError> {
    val response = try {
        execute()
    } catch (e: SocketTimeoutException) {
        return Result.Error(ApiError(DataError.Remote.REQUEST_TIMEOUT))
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        println("Error: $e")
        return Result.Error(ApiError(DataError.Remote.NO_INTERNET))
    }
    val status = response.status.value
    if (status in 200..299) {
        if (T::class == Unit::class) return Result.Success(Unit as T)
        return try {
            Result.Success(response.body<T>())
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Result.Error(ApiError(DataError.Remote.SERIALIZATION))
        }
    }
    val message = try {
        parseServerMessage(response.bodyAsText())
    } catch (e: Exception) {
        null
    }
    return Result.Error(ApiError(statusToRemote(status), message))
}

fun <T> Result<T, ApiError>.toRemote(): Result<T, DataError.Remote> = when (this) {
    is Result.Success -> this
    is Result.Error -> Result.Error(error.remote)
}
