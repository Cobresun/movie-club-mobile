package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.util.network.UnresolvedAddressException

/**
 * Maps a request failure to a typed error so the UI can say "check your connection"
 * or "server too slow" instead of a generic failure.
 *
 * Engines throw platform types (OkHttp: java.net.UnknownHostException / ConnectException;
 * Darwin: NSURLErrorDomain errors), so beyond the Ktor types this matches on the
 * cause chain's class names and messages.
 */
fun classifyNetworkException(e: Throwable): DataError.Remote {
    var current: Throwable? = e
    var depth = 0
    while (current != null && depth < 8) {
        when (current) {
            is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
                return DataError.Remote.REQUEST_TIMEOUT
            is UnresolvedAddressException, is OfflineException -> return DataError.Remote.NO_INTERNET
        }
        val name = current::class.simpleName.orEmpty()
        val message = current.message.orEmpty()
        if (OFFLINE_CLASS_NAMES.any { name.contains(it) } || OFFLINE_MESSAGES.any { message.contains(it, ignoreCase = true) }) {
            return DataError.Remote.NO_INTERNET
        }
        if (name.contains("Timeout") || message.contains("timed out", ignoreCase = true)) {
            return DataError.Remote.REQUEST_TIMEOUT
        }
        current = current.cause
        depth++
    }
    return DataError.Remote.UNKNOWN
}

private val OFFLINE_CLASS_NAMES = listOf("UnknownHost", "ConnectException", "NoRouteToHost", "UnresolvedAddress")
private val OFFLINE_MESSAGES = listOf(
    "Unable to resolve host",
    "Network is unreachable",
    "offline", // NSURLErrorNotConnectedToInternet: "The Internet connection appears to be offline."
    "Code=-1009",
    "Failed to connect",
)
