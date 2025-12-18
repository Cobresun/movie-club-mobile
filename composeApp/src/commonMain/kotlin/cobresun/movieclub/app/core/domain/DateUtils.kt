package cobresun.movieclub.app.core.domain

import kotlinx.datetime.LocalDate

/**
 * Extracts the year from an ISO 8601 date string (YYYY-MM-DD format).
 * Returns null if the date string is null or invalid.
 */
fun extractYearFromDateString(dateString: String?): String? {
    return dateString?.let {
        try {
            LocalDate.parse(it).year.toString()
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
