package cobresun.movieclub.app.stats

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.stats.data.CastCreditDto
import cobresun.movieclub.app.stats.data.toCreditedCast
import cobresun.movieclub.app.stats.domain.CreditedPerson
import cobresun.movieclub.app.stats.domain.ReviewerTitle
import cobresun.movieclub.app.stats.domain.computeClubRecords
import cobresun.movieclub.app.stats.domain.computeConsensus
import cobresun.movieclub.app.stats.domain.computeCumulativeCounts
import cobresun.movieclub.app.stats.domain.computeCurmudgeons
import cobresun.movieclub.app.stats.domain.computeGenreCounts
import cobresun.movieclub.app.stats.domain.computeGenreStats
import cobresun.movieclub.app.stats.domain.computeGuiltyPleasures
import cobresun.movieclub.app.stats.domain.computeLeaderboard
import cobresun.movieclub.app.stats.domain.computeMonthlyActivity
import cobresun.movieclub.app.stats.domain.computeScoreDistribution
import cobresun.movieclub.app.stats.domain.computeScoreSpreadTrend
import cobresun.movieclub.app.stats.domain.computeScoreTrend
import cobresun.movieclub.app.stats.domain.computeSubjectCounts
import cobresun.movieclub.app.stats.domain.computeSubjectStats
import cobresun.movieclub.app.stats.domain.computeSummary
import cobresun.movieclub.app.stats.domain.computeTasteSimilarity
import cobresun.movieclub.app.stats.domain.computeTmdbDeviation
import cobresun.movieclub.app.stats.domain.computeTopActors
import cobresun.movieclub.app.stats.domain.computeTopAuthors
import cobresun.movieclub.app.stats.domain.computeTopDirectors
import cobresun.movieclub.app.stats.domain.rollingWindow
import cobresun.movieclub.app.stats.domain.scoredReviews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsComputersTest {
    private val ann = Member("a", "a@x", "Ann Lee")
    private val bob = Member("b", "b@x", "Bob")
    private val cat = Member("c", "c@x", "Cat")
    private val members = listOf(ann, bob, cat)

    private fun movie(
        id: String,
        scores: Map<String, Double>,
        addedAt: String = "2025-01-15T12:00:00Z",
        genres: List<String> = emptyList(),
        directors: List<String> = emptyList(),
        runtime: Int? = null,
        tmdb: Double? = null,
    ) = review(id, scores, addedAt, WorkDetails.Movie(null, null, null, runtime, genres, directors, emptyList(), tmdb, null), WorkType.MOVIE)

    private fun book(id: String, scores: Map<String, Double>, subjects: List<String> = emptyList(), authors: List<String> = emptyList(), pages: Int? = null) =
        review(id, scores, "2025-01-15T12:00:00Z", WorkDetails.Book(null, authors, subjects, null, pages, null), WorkType.BOOK)

    private fun review(id: String, scores: Map<String, Double>, addedAt: String, details: WorkDetails?, type: WorkType): Review {
        val ms = scores.mapValues { (u, v) -> MemberScore("s$id$u", u, v, addedAt) }
        return Review(
            Work(id, type, "T$id", addedAt, null, null, "ext$id", null, details),
            ms,
            ms.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
        )
    }

    @Test fun scoredReviewsDropsUnscored() {
        val list = listOf(movie("1", mapOf("a" to 5.0)), movie("2", emptyMap()))
        assertEquals(listOf("1"), scoredReviews(list).map { it.work.id })
    }

    @Test fun summaryTotalsRuntimeAndPages() {
        val s = computeSummary(listOf(movie("1", mapOf("a" to 6.0), runtime = 100), movie("2", mapOf("a" to 8.0), runtime = 50)))
        assertEquals(2, s.workCount)
        assertEquals(150, s.runtimeMinutes)
        assertEquals(7.0, s.clubAverage)
        assertEquals(300, computeSummary(listOf(book("1", mapOf("a" to 5.0), pages = 300))).pageCount)
    }

    @Test fun recordsNeedTwoWorksAndFindSpread() {
        assertNull(computeClubRecords(listOf(movie("1", mapOf("a" to 5.0)))))
        val r = computeClubRecords(
            listOf(
                movie("1", mapOf("a" to 9.0, "b" to 9.0)),
                movie("2", mapOf("a" to 2.0, "b" to 8.0)),
                movie("3", mapOf("a" to 3.0)),
            )
        )!!
        assertEquals("1", r.highest.work.id)
        assertEquals("3", r.lowest.work.id)
        assertEquals("2", r.mostDivisive!!.work.id)
        assertEquals(3.0, r.mostDivisive.value)
    }

    @Test fun distributionBinsByFloorAndIgnoresNonMembers() {
        val d = computeScoreDistribution(listOf(movie("1", mapOf("a" to 7.5, "b" to 10.0, "z" to 3.0))), listOf(ann, bob))
        assertEquals(1, d.binsByMember.getValue("a")[7])
        assertEquals(1, d.binsByMember.getValue("b")[10])
        assertEquals(1, d.maxBinTotal)
    }

    @Test fun rollingWindowIsClamped() {
        assertEquals(5, rollingWindow(3))
        assertEquals(10, rollingWindow(50))
        assertEquals(30, rollingWindow(1000))
    }

    @Test fun scoreTrendSkipsMembersBelowWindow() {
        val reviews = (1..6).map { movie("$it", mapOf("a" to it.toDouble(), "b" to 5.0), addedAt = "2025-0$it-01T00:00:00Z") }
            .map { r -> if (r.work.id.toInt() > 2) r.copy(scores = r.scores - "b") else r }
        val trend = computeScoreTrend(reviews, members)
        assertEquals(setOf("a"), trend.keys)
        assertEquals(listOf(3.0, 4.0), trend.getValue("a").map { it.value })
    }

    @Test fun spreadTrendNeedsTwoScores() {
        val reviews = (1..5).map { movie("$it", mapOf("a" to 4.0, "b" to 6.0), addedAt = "2025-0$it-01T00:00:00Z") }
        assertEquals(listOf(1.0), computeScoreSpreadTrend(reviews).map { it.value })
        assertTrue(computeScoreSpreadTrend(listOf(movie("1", mapOf("a" to 4.0)))).isEmpty())
    }

    @Test fun monthlyActivityZeroFillsGaps() {
        val reviews = listOf(
            movie("1", mapOf("a" to 5.0), addedAt = "2024-11-03T00:00:00Z"),
            movie("2", mapOf("a" to 5.0), addedAt = "2025-02-10T00:00:00Z"),
            movie("3", mapOf("a" to 5.0), addedAt = "2025-02-11T00:00:00Z"),
        )
        val m = computeMonthlyActivity(reviews)
        assertEquals(listOf(11 to 2024, 12 to 2024, 1 to 2025, 2 to 2025), m.map { it.month to it.year })
        assertEquals(listOf(1, 0, 0, 2), m.map { it.count })
        assertEquals(listOf(1.0, 2.0, 3.0), computeCumulativeCounts(reviews.reversed()).map { it.value })
    }

    @Test fun genreStatsRequireTwoAndFilterByMember() {
        val reviews = listOf(
            movie("1", mapOf("a" to 9.0, "b" to 1.0), genres = listOf("Drama", "Horror")),
            movie("2", mapOf("a" to 7.0, "b" to 3.0), genres = listOf("Drama")),
            movie("3", mapOf("a" to 2.0), genres = listOf("Comedy")),
        )
        val club = computeGenreStats(reviews)
        assertEquals(listOf("Drama"), club.mostLoved.map { it.name })
        assertEquals(5.0, club.mostLoved.single().averageScore)
        assertEquals(8.0, computeGenreStats(reviews, "a").mostLoved.single().averageScore)
        assertEquals("Drama", computeGenreCounts(reviews).first().name)
    }

    @Test fun subjectStatsAndCounts() {
        val reviews = listOf(
            book("1", mapOf("a" to 8.0), subjects = listOf("Fiction", "Sci-fi")),
            book("2", mapOf("a" to 6.0), subjects = listOf("Fiction", "Sci-fi")),
            book("3", mapOf("a" to 2.0), subjects = listOf("Fiction")),
        )
        assertEquals(listOf("Sci-fi", "Fiction"), computeSubjectStats(reviews).map { it.name })
        assertEquals(CategoryCountPair("Fiction", 3), computeSubjectCounts(reviews).first().let { CategoryCountPair(it.name, it.count) })
    }

    private data class CategoryCountPair(val name: String, val count: Int)

    @Test fun peopleRankByCountThenScore() {
        val reviews = listOf(
            movie("1", mapOf("a" to 9.0), directors = listOf("Nolan")),
            movie("2", mapOf("a" to 7.0), directors = listOf("Nolan")),
            movie("3", mapOf("a" to 10.0), directors = listOf("Gerwig")),
        )
        val top = computeTopDirectors(reviews)
        assertEquals(listOf("Nolan", "Gerwig"), top.map { it.name })
        assertEquals(8.0, top.first().averageScore)
        val actors = computeTopActors(reviews, mapOf("ext1" to listOf(CreditedPerson("Cillian", "u"))))
        assertEquals("u", actors.single().imageUrl)
        assertEquals("Le Guin", computeTopAuthors(listOf(book("1", mapOf("a" to 5.0), authors = listOf("Le Guin")))).single().name)
    }

    @Test fun leaderboardTitlesSoftieAndHater() {
        val reviews = listOf(movie("1", mapOf("a" to 9.0, "b" to 5.0, "c" to 7.0)))
        val board = computeLeaderboard(reviews, members)
        assertEquals(listOf("a", "c", "b"), board.map { it.member.id })
        assertEquals(listOf(ReviewerTitle.SOFTIE, null, ReviewerTitle.HATER), board.map { it.title })
        assertNull(computeLeaderboard(reviews, listOf(ann)).single().title)
    }

    @Test fun tasteSimilarityPairsAndScopeOrientation() {
        val reviews = listOf(
            movie("1", mapOf("a" to 8.0, "b" to 8.0, "c" to 2.0)),
            movie("2", mapOf("a" to 6.0, "b" to 7.0, "c" to 9.0)),
            movie("3", mapOf("a" to 5.0, "b" to 5.0, "c" to 1.0)),
        )
        val all = computeTasteSimilarity(reviews, members)
        assertEquals(setOf("a", "b"), setOf(all.mostSimilar!!.memberA.id, all.mostSimilar.memberB.id))
        assertEquals(96.7, all.mostSimilar.similarityPercent)
        val forCat = computeTasteSimilarity(reviews, members, "c")
        assertEquals("c", forCat.mostSimilar!!.memberA.id)
        assertEquals("c", forCat.leastSimilar!!.memberA.id)
        // Scores follow the orientation.
        assertEquals(2.0, forCat.mostSimilar.bestAgreements.first { it.work.id == "1" }.scoreA)
        assertNull(computeTasteSimilarity(reviews.take(2), members).mostSimilar)
    }

    @Test fun consensusOrdersBySpread() {
        val reviews = listOf(
            movie("1", mapOf("a" to 5.0, "b" to 5.0)),
            movie("2", mapOf("a" to 1.0, "b" to 9.0)),
            movie("3", mapOf("a" to 5.0)),
        )
        val c = computeConsensus(reviews, members)
        assertEquals("1", c.mostAgreed.first().work.id)
        assertEquals("2", c.mostDivisive.first().work.id)
        assertEquals(4.0, c.mostDivisive.first().stdDev)
    }

    @Test fun outliersNeedASingleDeviant() {
        val reviews = listOf(
            movie("1", mapOf("a" to 9.0, "b" to 4.0, "c" to 4.0)), // avg 5.67: a +3.33
            movie("2", mapOf("a" to 9.0, "b" to 9.0, "c" to 1.0)), // avg 6.33: c -5.33
            movie("3", mapOf("a" to 10.0, "b" to 10.0, "c" to 1.0, )), // avg 7: two above? a,b +3 → skipped for guilty
        )
        val guilty = computeGuiltyPleasures(reviews, members)
        assertEquals(listOf("a"), guilty.map { it.member.id })
        assertEquals(listOf("1"), guilty.single().works.map { it.work.id })
        val grumpy = computeCurmudgeons(reviews, members)
        assertEquals(listOf("c"), grumpy.map { it.member.id })
        assertEquals(listOf("3", "2"), grumpy.single().works.map { it.work.id })
    }

    @Test fun tmdbDeviationSplitsAndSkipsMissing() {
        val reviews = listOf(
            movie("1", mapOf("a" to 9.0), tmdb = 6.0),
            movie("2", mapOf("a" to 4.0), tmdb = 7.5),
            movie("3", mapOf("a" to 4.0), tmdb = 0.0),
        )
        val d = computeTmdbDeviation(reviews)
        assertEquals(listOf(3.0), d.clubRatedHigher.map { it.deviation })
        assertEquals(listOf(-3.5), d.clubRatedLower.map { it.deviation })
    }

    @Test fun castDtoMapsProfileUrls() {
        val mapped = mapOf("1" to listOf(CastCreditDto("A", null, "/p.jpg"), CastCreditDto("B"))).toCreditedCast()
        assertEquals("https://image.tmdb.org/t/p/w185/p.jpg", mapped.getValue("1")[0].imageUrl)
        assertNull(mapped.getValue("1")[1].imageUrl)
    }
}
