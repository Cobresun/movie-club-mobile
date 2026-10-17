package cobresun.movieclub.app.auth.presentation

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Loose client-side email check (something@domain.tld); the server does the real validation. */
internal fun isPlausibleEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
