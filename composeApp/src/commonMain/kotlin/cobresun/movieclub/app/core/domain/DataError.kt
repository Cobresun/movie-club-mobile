package cobresun.movieclub.app.core.domain

sealed interface DataError: Error {
    enum class Remote: DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER,
        SERIALIZATION,
        EMAIL_NOT_CONFIRMED,
        UNKNOWN;

        /** Whether sending the same request again might succeed. */
        val isRetryable: Boolean
            get() = when (this) {
                NO_INTERNET, REQUEST_TIMEOUT, TOO_MANY_REQUESTS, SERVER, UNKNOWN -> true
                SERIALIZATION, EMAIL_NOT_CONFIRMED -> false
            }

        /**
         * Failures where the server's last answer beats an error screen: no connection, a timeout,
         * rate limiting or the server being down. Not "not found" or "forbidden" (those are answers).
         */
        val isConnectivityFailure: Boolean
            get() = when (this) {
                NO_INTERNET, REQUEST_TIMEOUT, TOO_MANY_REQUESTS, SERVER -> true
                SERIALIZATION, EMAIL_NOT_CONFIRMED, UNKNOWN -> false
            }
    }
}
