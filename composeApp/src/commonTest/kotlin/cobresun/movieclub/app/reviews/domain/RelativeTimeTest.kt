package cobresun.movieclub.app.reviews.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RelativeTimeTest {
    private val now = Instant.parse("2026-10-17T12:00:00Z")

    @Test
    fun usesTheLargestWholeUnit() {
        assertEquals("5 seconds ago", relativeTime("2026-10-17T11:59:55Z", now))
        assertEquals("1 minute ago", relativeTime("2026-10-17T11:58:30Z", now))
        assertEquals("3 hours ago", relativeTime("2026-10-17T09:00:00.000Z", now))
        assertEquals("2 days ago", relativeTime("2026-10-15T08:00:00Z", now))
        assertEquals("1 year ago", relativeTime("2025-10-01T12:00:00Z", now))
        assertEquals("in 2 hours", relativeTime("2026-10-17T14:00:00Z", now))
    }

    @Test
    fun rejectsUnparseableInput() {
        assertNull(relativeTime("yesterday", now))
    }
}
