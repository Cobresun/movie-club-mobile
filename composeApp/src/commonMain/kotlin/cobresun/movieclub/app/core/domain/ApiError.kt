package cobresun.movieclub.app.core.domain

/**
 * A remote failure with the context the server gave: its user-facing message
 * (e.g. slug taken, invite expired) and the HTTP status, when there was a response.
 */
data class ApiError(
    val remote: DataError.Remote,
    val serverMessage: String? = null,
    val httpCode: Int? = null,
) : Error
