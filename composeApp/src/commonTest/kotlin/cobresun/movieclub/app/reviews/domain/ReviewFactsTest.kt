package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import cobresun.movieclub.app.core.domain.groupThousands

class ReviewFactsTest {
    private fun movie(
        directors: List<String> = emptyList(),
        majorCast: List<String> = emptyList(),
        genres: List<String> = emptyList(),
        vote: Double? = null,
        runtime: Int? = null,
        release: String? = null,
    ) = WorkDetails.Movie(null, null, release, runtime, genres, directors, majorCast, vote, null, majorCastNames = majorCast)

    private fun book(authors: List<String> = emptyList(), pages: Int? = null, year: Int? = null) =
        WorkDetails.Book(null, authors, emptyList(), year, pages, null)

    private fun review(
        id: String,
        date: String,
        scores: Map<String, Double>,
        details: WorkDetails? = movie(),
        type: WorkType = WorkType.MOVIE,
    ) = Review(
        work = Work(id, type, "Work $id", date, null, null, null, null, details),
        scores = scores.mapValues { (u, v) -> MemberScore("$id-$u", u, v) },
        average = scores.values.takeIf { it.isNotEmpty() }?.average(),
    )

    /** [n] filler works in 2020 scored 5 (or [score]), with neutral metadata. */
    private fun history(n: Int, score: Double = 5.0, details: (Int) -> WorkDetails? = { movie() }) =
        (1..n).map { i -> review("h$i", "2020-01-${(i % 28 + 1).toString().padStart(2, '0')}T00:00:${i.toString().padStart(2, '0')}Z", mapOf("a" to score, "b" to score), details(i)) }

    @Test fun noFactWithoutAverage() {
        val reviews = history(12) + review("t", "2021-01-01T00:00:00Z", emptyMap())
        assertNull(ReviewFacts.compute(reviews, "t"))
    }

    @Test fun unknownWorkHasNoFact() = assertNull(ReviewFacts.compute(history(3), "nope"))

    @Test fun allTimeHighNeedsTenScoredWorks() {
        val few = history(8) + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 9.0, "b" to 9.0))
        assertNull(ReviewFacts.compute(few, "t"))
        val many = history(10) + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 9.0, "b" to 9.0))
        val fact = ReviewFacts.compute(many, "t")!!
        assertEquals(FactKind.ALL_TIME_HIGH, fact.kind)
        assertEquals("At 9, this is the highest-rated movie in club history.", fact.text)
    }

    @Test fun allTimeLowUsesBookNoun() {
        val books = history(10) { book() }.map { it.copy(work = it.work.copy(type = WorkType.BOOK)) } +
            review("t", "2021-01-01T00:00:00Z", mapOf("a" to 1.5, "b" to 2.0), book(), WorkType.BOOK)
        assertEquals("At 1.8, this is the lowest-rated book in club history.", ReviewFacts.compute(books, "t")!!.text)
    }

    @Test fun clubMilestoneAtTenth() {
        val reviews = history(9, 5.0) + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0))
        val fact = ReviewFacts.compute(reviews, "t")!!
        assertEquals(FactKind.CLUB_MILESTONE, fact.kind)
        assertEquals("This is the 10th movie your club has reviewed!", fact.text)
    }

    @Test fun watchTimeCrossesTwentyFourHours() {
        val reviews = history(3) { movie(runtime = 400) } + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), movie(runtime = 300))
        val fact = ReviewFacts.compute(reviews, "t")!!
        assertEquals(FactKind.WATCH_TIME_MILESTONE, fact.kind)
        assertEquals("This one tipped your club past 24 hours of movies watched together — a full day on the couch.", fact.text)
    }

    @Test fun pagesMilestoneGroupsThousands() {
        val reviews = history(3) { book(pages = 1500) }.map { it.copy(work = it.work.copy(type = WorkType.BOOK)) } +
            review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), book(pages = 600), WorkType.BOOK)
        assertEquals(
            "This one pushed your club past 5,000 pages read together — about 4 copies of War and Peace.",
            ReviewFacts.compute(reviews, "t")!!.text,
        )
    }

    @Test fun divisiveRecord() {
        val reviews = history(10) + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 2.0, "b" to 8.0))
        val fact = ReviewFacts.compute(reviews, "t")!!
        assertEquals(FactKind.DIVISIVE, fact.kind)
        assertEquals("Scores ran from 2 to 8 — the most divisive pick in club history.", fact.text)
    }

    @Test fun bestOfYear() {
        val old = history(10, 9.5) // 2020, higher scores block the all-time record
        val year = (1..4).map { review("y$it", "2022-0$it-01T00:00:00Z", mapOf("a" to 5.0)) }
        val fact = ReviewFacts.compute(old + year + review("t", "2022-06-01T00:00:00Z", mapOf("a" to 7.0)), "t")!!
        assertEquals(FactKind.YEAR_HIGH, fact.kind)
        assertEquals("Best of 2022", fact.label)
        assertEquals("The highest-rated of the 5 movies your club has reviewed in 2022.", fact.text)
    }

    @Test fun directorRecordNeedsThreeFilms() {
        val nolan = movie(directors = listOf("Christopher Nolan"))
        val two = listOf(review("n1", "2020-01-01T00:00:00Z", mapOf("a" to 6.0), nolan))
        assertNull(ReviewFacts.compute(two + review("t", "2020-02-01T00:00:00Z", mapOf("a" to 8.0), nolan), "t"))
        val three = two + review("n2", "2020-01-02T00:00:00Z", mapOf("a" to 7.0), nolan)
        val fact = ReviewFacts.compute(three + review("t", "2020-02-01T00:00:00Z", mapOf("a" to 8.0), nolan), "t")!!
        assertEquals(FactKind.PERSON_HIGH, fact.kind)
        assertEquals("Your club's highest-rated of 3 films directed by Christopher Nolan.", fact.text)
    }

    @Test fun familiarFaceCountsOnlyMajorCastThroughTarget() {
        val keanu = movie(majorCast = listOf("Keanu Reeves"))
        val prior = (1..4).map { review("k$it", "2020-01-0${it}T00:00:00Z", mapOf("a" to 5.0), keanu) }
        // A later Keanu movie must not change the 5th-movie fact.
        val later = review("k9", "2023-01-01T00:00:00Z", mapOf("a" to 5.0), keanu)
        val cameo = review("c", "2020-01-08T00:00:00Z", mapOf("a" to 5.0), movie())
        val fact = ReviewFacts.compute(prior + cameo + later + review("t", "2020-02-01T00:00:00Z", mapOf("a" to 5.0), keanu), "t")!!
        assertEquals(FactKind.ACTOR_MILESTONE, fact.kind)
        assertEquals("Familiar face", fact.label)
        assertEquals("That's the 5th movie your club has watched featuring Keanu Reeves.", fact.text)
    }

    @Test fun oldestMovieAndLongestRuntime() {
        val hist = history(10) { movie(release = "1990-01-01", runtime = 100) }
        val oldest = ReviewFacts.compute(hist + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), movie(release = "1960-05-05", runtime = 90)), "t")!!
        // 11th review: no count milestone; time travel beats the 1960s first-trip
        assertEquals("Released in 1960, this is the oldest movie your club has ever watched.", oldest.text)
        val longest = ReviewFacts.compute(hist + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), movie(release = "1995-01-01", runtime = 201)), "t")!!
        assertEquals("At 3h 21m, this is the longest movie your club has ever sat through.", longest.text)
    }

    @Test fun decadeAndGenreFirsts() {
        val hist = history(10) { movie(release = "1990-01-01", genres = listOf("Drama"), runtime = 100) }
        val decade = ReviewFacts.compute(hist + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), movie(release = "2005-01-01", genres = listOf("Drama"))), "t")!!
        assertEquals("Your club's first trip to the 2000s.", decade.text)
        val genre = ReviewFacts.compute(hist + review("t", "2021-01-01T00:00:00Z", mapOf("a" to 5.0), movie(release = "1995-01-01", genres = listOf("Drama", "Horror"))), "t")!!
        assertEquals("Your club's first Horror movie.", genre.text)
    }

    @Test fun tmdbHotTake() {
        val reviews = listOf(review("t", "2021-01-01T00:00:00Z", mapOf("a" to 9.0), movie(vote = 6.2)))
        assertEquals(
            "Your club scored this 2.8 points higher than its 6.2 TMDB rating.",
            ReviewFacts.compute(reviews, "t")!!.text,
        )
        assertNull(ReviewFacts.compute(listOf(review("t", "2021-01-01T00:00:00Z", mapOf("a" to 7.0), movie(vote = 6.2))), "t"))
    }

    @Test fun helpers() {
        assertEquals("11th", ReviewFacts.ordinal(11))
        assertEquals("22nd", ReviewFacts.ordinal(22))
        assertEquals("103rd", ReviewFacts.ordinal(103))
        assertEquals("250,000", groupThousands(250000))
        assertEquals("7.3", ReviewFacts.formatScore(7.25 + 0.04))
    }
}
