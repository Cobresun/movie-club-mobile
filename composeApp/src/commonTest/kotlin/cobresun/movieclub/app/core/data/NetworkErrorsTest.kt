package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkErrorsTest {
    private class UnknownHostException(message: String) : Exception(message)
    private class ConnectException(message: String) : Exception(message)

    @Test
    fun requestTimeoutIsTimeout() {
        assertEquals(
            DataError.Remote.REQUEST_TIMEOUT,
            classifyNetworkException(HttpRequestTimeoutException("https://x", 20_000)),
        )
    }

    @Test
    fun platformOfflineErrorsAreNoInternet() {
        assertEquals(DataError.Remote.NO_INTERNET, classifyNetworkException(UnknownHostException("movie-club.app")))
        assertEquals(DataError.Remote.NO_INTERNET, classifyNetworkException(ConnectException("refused")))
        assertEquals(
            DataError.Remote.NO_INTERNET,
            classifyNetworkException(Exception("Error Domain=NSURLErrorDomain Code=-1009 \"The Internet connection appears to be offline.\"")),
        )
    }

    @Test
    fun wrappedCauseIsInspected() {
        val wrapped = IllegalStateException("request failed", UnknownHostException("Unable to resolve host"))
        assertEquals(DataError.Remote.NO_INTERNET, classifyNetworkException(wrapped))
    }

    @Test
    fun otherErrorsAreUnknown() {
        assertEquals(DataError.Remote.UNKNOWN, classifyNetworkException(IllegalArgumentException("bad")))
    }
}
