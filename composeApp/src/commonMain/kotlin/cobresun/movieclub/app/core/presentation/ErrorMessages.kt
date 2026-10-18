package cobresun.movieclub.app.core.presentation

import cobresun.movieclub.app.core.domain.ApiError
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
        DataError.Remote.SERIALIZATION -> "Got an unexpected response. Try again."
        DataError.Remote.EMAIL_NOT_CONFIRMED -> "You don't have permission to do that. Try signing in again."
        DataError.Remote.UNKNOWN -> "Try again."
    }
    return "Couldn't $action. $hint"
}

/** Prefer the server's own message (e.g. "This url is already in use"), else [DataError.Remote.toUserMessage]. */
fun ApiError.toUserMessage(action: String): String =
    serverMessage?.takeIf { it.isNotBlank() } ?: remote.toUserMessage(action)

/** A one-off message for the snackbar, optionally with a retry action. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** The snackbar message for a failed [action], offering [retry] when retrying might help. */
fun DataError.Remote.toUiMessage(action: String, retry: (() -> Unit)? = null): UiMessage {
    val canRetry = retry != null && isRetryable
    return UiMessage(
        text = toUserMessage(action),
        actionLabel = if (canRetry) "Retry" else null,
        onAction = if (canRetry) retry else null,
    )
}
