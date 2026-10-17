package cobresun.movieclub.app.core

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkAddedYearTest {
    private fun work(addedAt: String) = Work(
        id = "1", type = WorkType.MOVIE, title = "T", addedAt = addedAt, addedDate = null,
        imageUrl = null, externalId = null, addedByUserId = null, details = null,
    )

    @Test
    fun bucketsByTheUtcYearLikeTheWeb() {
        assertEquals(2025, work("2025-12-31T23:30:00.000Z").addedYearUtc)
        assertEquals(2026, work("2025-12-31T20:30:00-05:00").addedYearUtc) // 01:30 UTC on Jan 1
        assertNull(work("not a date").addedYearUtc)
    }
}
