package cobresun.movieclub.app.club.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Error

/**
 * Error for club operations where the server returns a user-facing message
 * (e.g. slug taken, invite expired).
 */
data class ApiError(
    val remote: DataError.Remote,
    val serverMessage: String? = null,
) : Error
