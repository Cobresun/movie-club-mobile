package cobresun.movieclub.app.stats

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.stats.domain.computeGenreWatchCounts
import cobresun.movieclub.app.stats.domain.computeHighestRatedByYear
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsYearAndGenreTest {
    private fun movie(id: String, average: Double, addedAt: String = "2025-01-15T12:00:00Z", genres: List<String> = emptyList()): Review {
        val score = MemberScore("s$id", "a", average, addedAt)
        return Review(
            Work(id, WorkType.MOVIE, "T$id", addedAt, null, null, "ext$id", null,
                WorkDetails.Movie(null, null, null, null, genres, emptyList(), emptyList(), null, null)),
            mapOf("a" to score),
            average,
        )
    }

    @Test fun highestRatedByYearPicksEachYearsBestNewestFirst() {
        val reviews = listOf(
            movie("1", 7.0, "2024-02-01T00:00:00Z"),
            movie("2", 9.126, "2024-06-01T00:00:00Z"),
            movie("3", 8.0, "2025-01-01T00:00:00Z"),
            movie("4", 6.0, "2024-12-31T23:59:59Z"),
        )
        val best = computeHighestRatedByYear(reviews)
        assertEquals(listOf(2025, 2024), best.map { it.year })
        assertEquals(listOf("3", "2"), best.map { it.work.id })
        assertEquals(listOf(1, 3), best.map { it.workCount })
        assertEquals(9.13, best[1].average)
    }

    @Test fun highestRatedByYearKeepsTheFirstOnATie() {
        val best = computeHighestRatedByYear(listOf(movie("1", 8.0), movie("2", 8.0)))
        assertEquals("1", best.single().work.id)
    }

    @Test fun highestRatedByYearSkipsUndatedWorks() {
        assertEquals(emptyList(), computeHighestRatedByYear(listOf(movie("1", 8.0, "unknown"))))
    }

    @Test fun genreWatchCountsGiveTheMostAndLeastWatched() {
        val genres = listOf("A", "A", "A", "A", "A", "A", "B", "B", "B", "B", "B", "C", "C", "C", "C", "D", "D", "D", "E", "E", "F")
        val reviews = genres.mapIndexed { i, g -> movie("$i", 5.0, genres = listOf(g)) }
        val counts = computeGenreWatchCounts(reviews)
        assertEquals(listOf("A", "B", "C", "D", "E"), counts.mostWatched.map { it.name })
        assertEquals(listOf("F", "E", "D", "C", "B"), counts.leastWatched.map { it.name })
        assertEquals(listOf(1, 2, 3, 4, 5), counts.leastWatched.map { it.count })
    }
}
