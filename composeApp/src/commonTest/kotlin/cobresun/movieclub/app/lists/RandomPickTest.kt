package cobresun.movieclub.app.lists

import cobresun.movieclub.app.lists.domain.RandomPick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomPickTest {
    @Test
    fun intervalsSlowFromMinToMax() {
        val intervals = (1..RandomPick.CYCLES).map(RandomPick::intervalAfter)
        assertEquals(80L, intervals.first())
        assertEquals(400L, intervals.last())
        assertTrue(intervals.zipWithNext().all { (a, b) -> b >= a })
    }
}
