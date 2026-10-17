package cobresun.movieclub.app.reviews.domain

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * "3 hours ago" / "in 2 days" for an ISO-8601 instant, in the largest whole unit
 * (Luxon `toRelative()`, which the web's comment thread uses). Null when [iso]
 * doesn't parse.
 */
fun relativeTime(iso: String, now: Instant = Clock.System.now()): String? {
    val then = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val seconds = (now - then).inWholeSeconds
    val abs = kotlin.math.abs(seconds)
    val (value, unit) = when {
        abs >= 365L * 86_400 -> abs / (365L * 86_400) to "year"
        abs >= 30L * 86_400 -> abs / (30L * 86_400) to "month"
        abs >= 86_400 -> abs / 86_400 to "day"
        abs >= 3_600 -> abs / 3_600 to "hour"
        abs >= 60 -> abs / 60 to "minute"
        else -> abs to "second"
    }
    val label = "$value $unit${if (value == 1L) "" else "s"}"
    return if (seconds >= 0) "$label ago" else "in $label"
}
