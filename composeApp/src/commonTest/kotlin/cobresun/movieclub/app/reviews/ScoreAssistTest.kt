package cobresun.movieclub.app.reviews

import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.reviews.domain.ComparisonAnswer
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import cobresun.movieclub.app.reviews.domain.ScoredWork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreAssistTest {
    private fun work(id: String, score: Double, ext: String? = id, type: WorkType = WorkType.MOVIE, date: String = "2025-01-01", club: String = "c1") =
        ScoredWork(id, type, "T$id", null, ext, emptyList(), score, club, "Club", "club", date)

    private val target = ScoreAssist.Target("target", WorkType.MOVIE, "tx", emptyList())

    @Test
    fun poolFiltersTypeTargetAndDedupesByExternalIdKeepingLatest() {
        val pool = ScoreAssist.buildCandidatePool(
            listOf(
                work("a", 5.0, ext = "e1", date = "2024-01-01"),
                work("b", 7.0, ext = "e1", date = "2025-01-01", club = "c2"),
                work("book", 3.0, type = WorkType.BOOK),
                work("target", 4.0),
                work("same-ext", 4.0, ext = "tx"),
                work("noext1", 2.0, ext = null),
                work("noext2", 2.0, ext = null),
            ),
            target,
        )
        assertEquals(listOf("noext1", "noext2", "b"), pool.map { it.workId })
    }

    @Test
    fun eligibilityNeedsFiveWorks() {
        assertFalse(ScoreAssist.isEligible(List(4) { work("$it", it.toDouble()) }))
        assertTrue(ScoreAssist.isEligible(List(5) { work("$it", it.toDouble()) }))
    }

    @Test
    fun likingMoreThanEverythingSuggestsJustAboveTop() {
        val pool = (0..9).map { work("w$it", it.toDouble()) }
        var s = ScoreAssist.start(target, pool)
        while (s.result == null) s = ScoreAssist.answer(s, ComparisonAnswer.MORE)
        val r = assertNotNull(s.result)
        assertTrue(r.suggestedScore > 8.0 && r.suggestedScore <= 10.0)
        assertTrue(s.comparisons <= ScoreAssist.MAX_COMPARISONS)
    }

    @Test
    fun sameMatchesPivotScore() {
        val pool = (0..9).map { work("w$it", it.toDouble()) }
        val s = ScoreAssist.start(target, pool)
        val pivot = assertNotNull(s.pivot)
        val done = ScoreAssist.answer(s, ComparisonAnswer.SAME)
        assertEquals(pivot.score, done.result?.suggestedScore)
        assertEquals(ScoreAssist.Result.Kind.MATCHED, done.result?.kind)
        assertNull(done.pivot)
    }

    @Test
    fun skipNeverReoffersAndTerminates() {
        val pool = (0..5).map { work("w$it", it * 2.0) }
        var s = ScoreAssist.start(target, pool)
        val seen = mutableSetOf<String>()
        while (s.result == null) {
            assertTrue(seen.add(s.pivot!!.workId))
            s = ScoreAssist.answer(s, ComparisonAnswer.SKIP)
        }
        assertNotNull(s.result)
    }
}
