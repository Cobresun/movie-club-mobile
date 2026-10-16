package cobresun.movieclub.app.member.presentation.components

import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError

/** User-facing text for a remote failure, with a retry hint where retrying can help. */
fun DataError.Remote.toUserMessage(action: String): String = when (this) {
    DataError.Remote.NO_INTERNET -> "Couldn't $action. Check your connection and try again."
    DataError.Remote.REQUEST_TIMEOUT -> "Couldn't $action: the server took too long. Try again."
    DataError.Remote.TOO_MANY_REQUESTS -> "Too many attempts. Wait a moment and try again."
    DataError.Remote.SERVER -> "Couldn't $action: something went wrong on our end. Try again later."
    DataError.Remote.EMAIL_NOT_CONFIRMED -> "You don't have permission to $action."
    else -> "Couldn't $action. Try again."
}

/** Prefer the server's own message (e.g. "This url is already in use"), else a generic one. */
fun ApiError.toUserMessage(action: String): String =
    serverMessage?.takeIf { it.isNotBlank() } ?: remote.toUserMessage(action)
