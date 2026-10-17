package cobresun.movieclub.app.reviews

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.reviews.data.dto.ReviewItemDto
import cobresun.movieclub.app.reviews.data.dto.ScoreDto
import cobresun.movieclub.app.reviews.data.mappers.toReview
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.sortReviews
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewsDomainTest {

    private fun review(
        id: String,
        title: String,
        addedAt: String,
        scores: Map<String, Double> = emptyMap(),
        director: String? = null,
    ): Review {
        val memberScores = scores.mapValues { (user, v) -> MemberScore("s-$id-$user", user, v) }
        return Review(
            work = Work(
                id = id, type = WorkType.MOVIE, title = title, addedAt = addedAt, addedDate = null,
                imageUrl = null, externalId = id, addedByUserId = null,
                details = director?.let {
                    WorkDetails.Movie(null, null, null, null, listOf("Drama"), listOf(it), emptyList(), null, null)
                },
            ),
            scores = memberScores,
            average = memberScores.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
        )
    }

    private val a = review("a", "Alien", "2024-01-01T00:00:00Z", mapOf("u1" to 9.0, "u2" to 7.0), director = "Ridley Scott")
    private val b = review("b", "Brazil", "2024-03-01T00:00:00Z", mapOf("u1" to 6.0))
    private val c = review("c", "Casablanca", "2024-02-01T00:00:00Z")
    private val all = listOf(b, c, a) // server order: newest first

    @Test
    fun noneKeepsServerOrder() {
        assertEquals(all, sortReviews(all, ReviewSortState()))
    }

    @Test
    fun sortsByDateBothWays() {
        assertEquals(listOf("b", "c", "a"), sortReviews(all, ReviewSortState(ReviewSortOption.DateReviewed, true)).ids())
        assertEquals(listOf("a", "c", "b"), sortReviews(all, ReviewSortState(ReviewSortOption.DateReviewed, false)).ids())
    }

    @Test
    fun unscoredSinkInEitherDirection() {
        assertEquals(listOf("a", "b", "c"), sortReviews(all, ReviewSortState(ReviewSortOption.AverageScore, true)).ids())
        assertEquals(listOf("b", "a", "c"), sortReviews(all, ReviewSortState(ReviewSortOption.AverageScore, false)).ids())
        // Only Alien has u2's score.
        assertEquals(listOf("a", "b", "c"), sortReviews(all, ReviewSortState(ReviewSortOption.MemberScore("u2"), true)).ids())
    }

    @Test
    fun directionLabelDependsOnWhatIsSorted() {
        assertEquals("", ReviewSortState().directionLabel)
        assertEquals("Newest first", ReviewSortState(ReviewSortOption.DateReviewed, true).directionLabel)
        assertEquals("Oldest first", ReviewSortState(ReviewSortOption.DateReviewed, false).directionLabel)
        assertEquals("Highest first", ReviewSortState(ReviewSortOption.AverageScore, true).directionLabel)
        assertEquals("Lowest first", ReviewSortState(ReviewSortOption.MemberScore("u1"), false).directionLabel)
    }

    @Test
    fun withScoreRecomputesAverage() {
        val updated = a.withScore("u2", MemberScore("x", "u2", 10.0))
        assertEquals(9.5, updated.average)
        val removed = updated.withScore("u1", null).withScore("u2", null)
        assertNull(removed.average)
        assertTrue(removed.scores.isEmpty())
    }

    @Test
    fun mapperSplitsAverageFromMemberScores() {
        val dto = ReviewItemDto(
            id = "w1",
            title = "Inception",
            createdDate = "2026-10-16T13:38:09.999Z",
            scores = mapOf(
                "u1" to ScoreDto("s1", 7.5, "2026-10-16T13:38:12.497Z"),
                "average" to ScoreDto("average", 7.5, "2026-10-16T13:38:12.960Z"),
            ),
        )
        val review = dto.toReview(TimeZone.UTC)
        assertEquals(setOf("u1"), review.scores.keys)
        assertEquals("s1", review.scores.getValue("u1").id)
        assertEquals(7.5, review.average)

        val unscored = dto.copy(scores = emptyMap()).toReview(TimeZone.UTC)
        assertNull(unscored.average)
    }

    @Test
    fun scoreFormattingAndSnapping() {
        assertEquals("7.5", ScoreScale.format(7.5))
        assertEquals("8", ScoreScale.format(8.0))
        assertEquals("8.25", ScoreScale.format(8.25))
        assertEquals("6.67", ScoreScale.format(6.666666))
        assertEquals(7.5, ScoreScale.snap(7.4))
        assertEquals(10.0, ScoreScale.snap(11.0))
        assertEquals(0.0, ScoreScale.snap(-1.0))
    }

    @Test
    fun scoreInputSanitization() {
        assertEquals("8", ScoreScale.sanitizeInput("08"))
        assertEquals("7.55", ScoreScale.sanitizeInput("007.555"))
        assertEquals("10", ScoreScale.sanitizeInput("11"))
        assertEquals("8.", ScoreScale.sanitizeInput("8."))
        assertEquals("", ScoreScale.sanitizeInput(""))
        assertEquals("0.5", ScoreScale.sanitizeInput("0.5"))
        assertEquals("7.5", ScoreScale.sanitizeInput("7,5"))
        assertEquals("1.23", ScoreScale.sanitizeInput("1.2.3"))
        assertTrue(ScoreScale.isValid(0.0))
        assertFalse(ScoreScale.isValid(10.5))
        assertFalse(ScoreScale.isValid(null))
    }

    private fun List<Review>.ids() = map { it.work.id }
}
