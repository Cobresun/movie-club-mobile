package cobresun.movieclub.app.stats

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.stats.domain.VolumeUnit
import cobresun.movieclub.app.stats.domain.WrappedCardContent
import cobresun.movieclub.app.stats.domain.WrappedContext
import cobresun.movieclub.app.stats.domain.WrappedTone
import cobresun.movieclub.app.stats.domain.activeMemberCount
import cobresun.movieclub.app.stats.domain.buildWrappedDeck
import cobresun.movieclub.app.stats.domain.computeClosestMatches
import cobresun.movieclub.app.stats.domain.computeHotTakes
import cobresun.movieclub.app.stats.domain.computePicks
import cobresun.movieclub.app.stats.domain.isYearReleased
import cobresun.movieclub.app.stats.domain.worksInYear
import cobresun.movieclub.app.stats.domain.wrappedYears
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class WrappedTest {
    private val alice = Member("a", "a@x", "Alice Smith")
    private val bob = Member("b", "b@x", "Bob Jones")
    private val cara = Member("c", "c@x", "Cara Lee")
    private val dan = Member("d", "d@x", "Dan Wu")
    private val trio = listOf(alice, bob, cara)

    private fun movie(
        id: String,
        scores: Map<String, Double> = mapOf("a" to 5.0),
        addedAt: String = "2025-06-01T00:00:00Z",
        title: String = "T$id",
        runtime: Int? = null,
        directors: List<String> = emptyList(),
    ): Review = review(
        id, title, scores, addedAt, WorkType.MOVIE,
        WorkDetails.Movie(null, null, null, runtime, emptyList(), directors, emptyList(), null, null),
    )

    private fun book(id: String, scores: Map<String, Double>, pages: Int? = null, authors: List<String> = emptyList()) =
        review(id, "T$id", scores, "2025-06-01T00:00:00Z", WorkType.BOOK, WorkDetails.Book(null, authors, emptyList(), null, pages, null))

    private fun review(id: String, title: String, scores: Map<String, Double>, addedAt: String, type: WorkType, details: WorkDetails): Review {
        val ms = scores.mapValues { (u, v) -> MemberScore("s$id$u", u, v, addedAt) }
        return Review(
            Work(id, type, title, addedAt, null, "img$id", "ext$id", null, details),
            ms,
            ms.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
        )
    }

    private fun withAverage(id: String, average: Double) = movie(id, mapOf("a" to average), title = "W$id")

    // --- Years ----------------------------------------------------------------

    @Test fun wrappedYearsListsEachYearOnceNewestFirst() {
        val works = listOf(
            movie("1", addedAt = "2024-03-01T00:00:00.000Z"),
            movie("2", addedAt = "2025-06-01T00:00:00.000Z"),
            movie("3", addedAt = "2024-11-01T00:00:00.000Z"),
        )
        assertEquals(listOf(2025, 2024), wrappedYears(works, Instant.parse("2026-03-01T00:00:00Z")))
    }

    @Test fun wrappedYearsHoldsBackTheCurrentYearUntilJanuaryFirst() {
        val works = listOf(
            movie("1", addedAt = "2025-06-01T00:00:00.000Z"),
            movie("2", addedAt = "2026-03-01T00:00:00.000Z"),
        )
        assertEquals(listOf(2025), wrappedYears(works, Instant.parse("2026-12-31T23:59:59.000Z")))
        assertEquals(listOf(2026, 2025), wrappedYears(works, Instant.parse("2027-01-01T00:00:00.000Z")))
    }

    @Test fun reviewAtMidnightUtcOnNewYearsDayCountsForTheNewYear() {
        val newYear = movie("1", addedAt = "2025-01-01T00:00:00.000Z")
        assertEquals(listOf(newYear), worksInYear(listOf(newYear), 2025))
        assertEquals(emptyList(), worksInYear(listOf(newYear), 2024))
    }

    @Test fun reviewLateOnNewYearsEveInUtcStaysInTheOldYear() {
        val eve = movie("1", addedAt = "2024-12-31T23:59:59.000Z")
        assertEquals(listOf(eve), worksInYear(listOf(eve), 2024))
    }

    @Test fun unparseableDatesBelongToNoYear() {
        assertEquals(emptyList(), wrappedYears(listOf(movie("1", addedAt = "not a date")), Instant.parse("2030-01-01T00:00:00Z")))
    }

    @Test fun yearIsReleasedOnJanuaryFirstOfTheNext() {
        val newYearsEve = Instant.parse("2026-12-31T23:59:59.000Z")
        val newYearsDay = Instant.parse("2027-01-01T00:00:00.000Z")
        assertTrue(isYearReleased(2025, newYearsEve))
        assertFalse(isYearReleased(2026, newYearsEve))
        assertTrue(isYearReleased(2026, newYearsDay))
        assertFalse(isYearReleased(2027, newYearsDay))
    }

    // --- Picks ----------------------------------------------------------------

    @Test fun picksTakeTheThreeBestAndThreeWorstOfAFullYear() {
        val (top, bottom) = computePicks(listOf(5.0, 9.0, 1.0, 7.0, 3.0, 8.0, 2.0).mapIndexed { i, a -> withAverage("$i", a) })
        assertEquals(listOf(9.0, 8.0, 7.0), top.map { it.average })
        assertEquals(listOf(1.0, 2.0, 3.0), bottom.map { it.average })
    }

    @Test fun picksNeverShowTheSameWorkAsTopAndBottom() {
        val (top, bottom) = computePicks(listOf(4.0, 8.0, 6.0).mapIndexed { i, a -> withAverage("$i", a) })
        assertEquals(listOf(8.0), top.map { it.average })
        assertEquals(listOf(4.0), bottom.map { it.average })
    }

    @Test fun picksAreEmptyForASingleWork() {
        val picks = computePicks(listOf(withAverage("1", 7.0)))
        assertTrue(picks.top.isEmpty() && picks.bottom.isEmpty())
    }

    @Test fun picksBreakTiesByTitle() {
        val works = listOf(movie("1", mapOf("a" to 7.0), title = "Zodiac"), movie("2", mapOf("a" to 7.0), title = "alien"), withAverage("3", 1.0), withAverage("4", 2.0))
        assertEquals(listOf("alien", "Zodiac"), computePicks(works).top.map { it.work.title })
    }

    // --- Hot takes --------------------------------------------------------------

    @Test fun hotTakesFindEachMembersScoreFurthestFromEveryoneElse() {
        val works = listOf(
            movie("1", mapOf("a" to 9.0, "b" to 2.0, "c" to 3.0), title = "Cats"),
            movie("2", mapOf("a" to 8.0, "b" to 8.0, "c" to 3.0), title = "Heat"),
        )
        val takes = computeHotTakes(works, trio)
        assertEquals(
            listOf(Triple("Alice Smith", "Cats", 6.5), Triple("Cara Lee", "Heat", -5.0), Triple("Bob Jones", "Cats", -4.0)),
            takes.map { Triple(it.member.name, it.review.work.title, it.gap) },
        )
        assertEquals(2.5, takes.first().othersAverage)
        assertEquals(9.0, takes.first().memberScore)
    }

    @Test fun memberWhoAlwaysAgreedHasNoHotTake() {
        assertEquals(emptyList(), computeHotTakes(listOf(movie("1", mapOf("a" to 7.0, "b" to 7.0))), listOf(alice, bob)))
    }

    @Test fun hotTakesSkipWorksNobodyElseScored() {
        assertEquals(emptyList(), computeHotTakes(listOf(movie("1", mapOf("a" to 1.0))), listOf(alice, bob)))
    }

    // --- Closest matches ----------------------------------------------------------

    @Test fun closestMatchesPairEachMemberWithTheirMostAlikeClubmate() {
        val works = listOf("1", "2", "3").map { movie(it, mapOf("a" to 8.0, "b" to 7.0, "c" to 2.0)) }
        val matches = computeClosestMatches(works, trio)
        assertEquals(
            listOf("Alice Smith" to "Bob Jones", "Bob Jones" to "Alice Smith", "Cara Lee" to "Bob Jones"),
            matches.map { it.member.name to it.match.name },
        )
        assertEquals(listOf(90.0, 90.0, 50.0), matches.map { it.similarityPercent })
    }

    @Test fun activeMembersAreThoseWhoScoredSomething() {
        assertEquals(2, activeMemberCount(listOf(movie("1", mapOf("a" to 1.0, "c" to 2.0))), trio + dan))
    }

    // --- Deck -----------------------------------------------------------------

    private fun deck(reviews: List<Review>, members: List<Member> = trio, type: ClubType = ClubType.MOVIE) =
        buildWrappedDeck(WrappedContext(2025, type, reviews, members))

    @Test fun deckForASingleWorkIsJustTheIntroAndWhatItCanSay() {
        val cards = deck(listOf(movie("1", mapOf("a" to 6.0, "b" to 8.0), runtime = 120)))
        assertEquals(listOf("intro", "hours-watched", "hot-takes"), cards.map { it.id })
        val intro = cards.first().content as WrappedCardContent.Intro
        assertEquals(1, intro.workCount)
        assertEquals(2, intro.memberCount)
        assertEquals("movies watched", intro.countLabel)
        assertEquals(WrappedTone.VIOLET, cards.first().tone)
    }

    @Test fun fullMovieDeckFollowsTheWebOrder() {
        val works = (1..6).map { i ->
            movie("$i", mapOf("a" to i.toDouble(), "b" to (10 - i).toDouble(), "c" to 5.0), runtime = 100, directors = if (i <= 2) listOf("Nolan") else emptyList())
        }
        assertEquals(
            listOf("intro", "hours-watched", "top-picks", "bottom-picks", "most-divisive", "top-director", "hot-takes", "taste-twins", "closest-matches"),
            deck(works).map { it.id },
        )
    }

    @Test fun bookDeckUsesPagesAndAuthors() {
        val works = listOf(
            book("1", mapOf("a" to 8.0, "b" to 4.0), pages = 300, authors = listOf("Le Guin")),
            book("2", mapOf("a" to 6.0, "b" to 6.0), pages = 250, authors = listOf("Le Guin")),
        )
        val cards = deck(works, listOf(alice, bob), ClubType.BOOK)
        assertEquals(listOf("intro", "pages-read", "top-picks", "bottom-picks", "most-divisive", "top-author", "hot-takes"), cards.map { it.id })
        val pages = cards[1].content as WrappedCardContent.Volume
        assertEquals(VolumeUnit.PAGES, pages.unit)
        assertEquals(550, pages.value)
        assertEquals("Across 2 books.", pages.detail)
        assertEquals("Most-read author", cards[5].label)
        assertEquals("books", (cards[5].content as WrappedCardContent.Person).countNoun)
        assertEquals("books read", (cards[0].content as WrappedCardContent.Intro).countLabel)
    }

    @Test fun timeTogetherRoundsToHoursAndMentionsDaysOnceThereIsOne() {
        val short = deck(listOf(movie("1", runtime = 90)))[1].content as WrappedCardContent.Volume
        assertEquals(2, short.value)
        assertEquals("hours watched", short.unitLabel)
        assertEquals("Across 1 movies.", short.detail)

        val long = deck((1..12).map { movie("$it", runtime = 150) })[1].content as WrappedCardContent.Volume
        assertEquals(30, long.value)
        assertEquals("12 movies, or 1.3 days of non-stop viewing.", long.detail)
    }

    @Test fun timeTogetherIsDroppedWithoutRuntimes() {
        assertFalse(deck(listOf(movie("1"))).any { it.id == "hours-watched" })
    }

    @Test fun mostDivisiveNeedsTwoWorksAndSomeSpread() {
        assertFalse(deck(listOf(movie("1", mapOf("a" to 1.0, "b" to 9.0)))).any { it.id == "most-divisive" })
        val agreed = listOf(movie("1", mapOf("a" to 5.0, "b" to 5.0)), movie("2", mapOf("a" to 7.0, "b" to 7.0)))
        assertFalse(deck(agreed).any { it.id == "most-divisive" })

        val split = listOf(movie("1", mapOf("a" to 5.0, "b" to 5.0)), movie("2", mapOf("a" to 2.0, "b" to 9.0)))
        val card = deck(split).first { it.id == "most-divisive" }.content as WrappedCardContent.Divisive
        assertEquals("2", card.review.work.id)
        assertEquals(3.5, card.spread)
        assertEquals(5.5, card.average)
        assertEquals(listOf(9.0, 2.0), card.scores.map { it.score })
    }

    @Test fun directorNeedsTwoWorks() {
        val once = listOf(movie("1", directors = listOf("Nolan")), movie("2", directors = listOf("Lynch")))
        assertFalse(deck(once).any { it.id == "top-director" })
        val twice = listOf(movie("1", mapOf("a" to 8.0), directors = listOf("Nolan")), movie("2", mapOf("a" to 6.0), directors = listOf("Nolan")))
        val person = deck(twice).first { it.id == "top-director" }.content as WrappedCardContent.Person
        assertEquals("Nolan", person.name)
        assertEquals(2, person.workCount)
        assertEquals(7.0, person.averageScore)
        assertEquals("movies", person.countNoun)
    }

    @Test fun hotTakesOverflowOntoNumberedCards() {
        val members = (1..5).map { Member("m$it", "m$it@x", "Member $it") }
        val works = listOf(movie("1", members.mapIndexed { i, m -> m.id to (i * i).toDouble() }.toMap()))
        val cards = deck(works, members).filter { it.id.startsWith("hot-takes") }
        assertEquals(listOf("hot-takes-1", "hot-takes-2"), cards.map { it.id })
        assertEquals(listOf("Hot takes (1 of 2)", "Hot takes (2 of 2)"), cards.map { it.label })
        assertEquals(listOf(4, 1), cards.map { (it.content as WrappedCardContent.HotTakes).takes.size })
    }

    @Test fun tasteTwinsDropTheOppositesWhenThereIsOnlyOnePair() {
        val works = (1..3).map { movie("$it", mapOf("a" to 8.0, "b" to 6.0)) }
        val card = deck(works, listOf(alice, bob)).first { it.id == "taste-twins" }.content as WrappedCardContent.TasteTwins
        assertNull(card.opposites)
        assertEquals(80.0, card.twins.similarityPercent)
        assertEquals("movies", card.countNoun)
    }

    @Test fun tasteTwinsKeepTheOppositesForADifferentPair() {
        val works = (1..3).map { movie("$it", mapOf("a" to 8.0, "b" to 7.0, "c" to 1.0)) }
        val card = deck(works).first { it.id == "taste-twins" }.content as WrappedCardContent.TasteTwins
        assertEquals(alice to bob, card.twins.memberA to card.twins.memberB)
        assertEquals(alice to cara, card.opposites?.let { it.memberA to it.memberB })
    }

    @Test fun closestMatchesOnlyForClubsOfMoreThanTwoAndFivePerCard() {
        val pairWorks = (1..3).map { movie("$it", mapOf("a" to 8.0, "b" to 6.0)) }
        assertFalse(deck(pairWorks, listOf(alice, bob)).any { it.id.startsWith("closest-matches") })

        val members = (1..6).map { Member("m$it", "m$it@x", "Member $it") }
        val works = (1..3).map { w -> movie("$w", members.mapIndexed { i, m -> m.id to i.toDouble() }.toMap()) }
        val cards = deck(works, members).filter { it.id.startsWith("closest-matches") }
        assertEquals(listOf("closest-matches-1", "closest-matches-2"), cards.map { it.id })
        assertEquals(listOf(5, 1), cards.map { (it.content as WrappedCardContent.ClosestMatches).matches.size })
    }

    @Test fun deckForAYearOnlyUsesThatYearsWorks() {
        val reviews = listOf(movie("1", addedAt = "2024-05-01T00:00:00Z"), movie("2", addedAt = "2025-05-01T00:00:00Z"))
        val cards = buildWrappedDeck(2025, ClubType.MOVIE, reviews, trio)
        assertEquals(1, (cards.first().content as WrappedCardContent.Intro).workCount)
        assertEquals(emptyList(), buildWrappedDeck(2023, ClubType.MOVIE, reviews, trio))
    }
}
