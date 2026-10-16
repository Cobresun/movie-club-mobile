package cobresun.movieclub.app.core.presentation

import cobresun.movieclub.app.core.domain.DataError

/**
 * A user-facing sentence explaining why [action] failed and what to do next,
 * e.g. `"Couldn't save your score. Check your connection and try again."`.
 */
fun DataError.Remote.toUserMessage(action: String): String {
    val hint = when (this) {
        DataError.Remote.NO_INTERNET -> "Check your connection and try again."
        DataError.Remote.REQUEST_TIMEOUT -> "The server took too long to respond. Try again."
        DataError.Remote.TOO_MANY_REQUESTS -> "Too many requests right now. Wait a moment and try again."
        DataError.Remote.SERVER -> "The server had a problem. Try again in a bit."
        DataError.Remote.SERIALIZATION -> "Got an unexpected response. Pull to refresh and try again."
        DataError.Remote.EMAIL_NOT_CONFIRMED -> "You don't have permission to do that. Try signing in again."
        DataError.Remote.UNKNOWN -> "Pull to refresh and try again."
    }
    return "Couldn't $action. $hint"
}

/** Whether retrying the same request might succeed. */
val DataError.Remote.isRetryable: Boolean
    get() = when (this) {
        DataError.Remote.NO_INTERNET,
        DataError.Remote.REQUEST_TIMEOUT,
        DataError.Remote.TOO_MANY_REQUESTS,
        DataError.Remote.SERVER,
        DataError.Remote.UNKNOWN -> true

        DataError.Remote.SERIALIZATION,
        DataError.Remote.EMAIL_NOT_CONFIRMED -> false
    }

/** A one-off message for the snackbar, optionally with a retry action. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)
