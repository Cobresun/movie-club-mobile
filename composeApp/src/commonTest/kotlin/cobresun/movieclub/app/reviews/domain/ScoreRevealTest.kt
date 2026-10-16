package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreRevealTest {

    private fun review(id: String, scores: Map<String, Double>): Review {
        val memberScores = scores.mapValues { (user, v) -> MemberScore("s-$id-$user", user, v) }
        return Review(
            work = Work(
                id = id, type = WorkType.MOVIE, title = id, addedAt = "2024-01-01T00:00:00Z", addedDate = null,
                imageUrl = null, externalId = id, addedByUserId = null, details = null,
            ),
            scores = memberScores,
            average = memberScores.values.takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
        )
    }

    private val unscoredByMe = review("a", mapOf("u2" to 7.0))
    private val scoredByMe = review("b", mapOf("me" to 8.0, "u2" to 6.0))

    @Test
    fun hiddenUntilScoredOrRevealed() {
        assertFalse(ScoreReveal.isRevealed(unscoredByMe, "me", emptySet()))
        assertTrue(ScoreReveal.isRevealed(scoredByMe, "me", emptySet()))
        assertTrue(ScoreReveal.isRevealed(unscoredByMe, "me", setOf("a")))
    }

    @Test
    fun signedOutRevealsNothingUnlessRevealedByHand() {
        assertFalse(ScoreReveal.isRevealed(scoredByMe, null, emptySet()))
        assertTrue(ScoreReveal.isRevealed(scoredByMe, null, setOf("b")))
    }

    @Test
    fun ownScoreIsNeverHidden() {
        assertFalse(ScoreReveal.isHidden("me", "me", revealed = false))
        assertTrue(ScoreReveal.isHidden("u2", "me", revealed = false))
        // The average (null member) gives others away, so it hides too.
        assertTrue(ScoreReveal.isHidden(null, "me", revealed = false))
        assertFalse(ScoreReveal.isHidden("u2", "me", revealed = true))
        assertFalse(ScoreReveal.isHidden(null, "me", revealed = true))
    }

    @Test
    fun everythingIsOthersWhenSignedOut() {
        assertTrue(ScoreReveal.isOthersScore("me", null))
        assertTrue(ScoreReveal.isOthersScore(null, null))
    }

    @Test
    fun revealOfferedOnlyWhileSomethingIsHidden() {
        assertTrue(ScoreReveal.canReveal(unscoredByMe, "me", emptySet()))
        assertFalse(ScoreReveal.canReveal(unscoredByMe, "me", setOf("a")))
        assertFalse(ScoreReveal.canReveal(scoredByMe, "me", emptySet()))
        assertFalse(ScoreReveal.canReveal(review("c", emptyMap()), "me", emptySet()))
    }
}

class TmdbRatingRevealTest {
    private fun review(scores: Map<String, Double>) = Review(
        work = Work(
            id = "w", type = WorkType.MOVIE, title = "w", addedAt = "2024-01-01T00:00:00Z", addedDate = null,
            imageUrl = null, externalId = "w", addedByUserId = null, details = null,
        ),
        scores = scores.mapValues { (u, v) -> MemberScore("s-$u", u, v) },
        average = scores.values.takeIf { it.isNotEmpty() }?.average(),
    )

    @Test
    fun hiddenUntilScoredOrTapped() {
        assertTrue(ScoreReveal.isTmdbRatingHidden(review(mapOf("u2" to 5.0)), "me", tappedToReveal = false))
        assertFalse(ScoreReveal.isTmdbRatingHidden(review(mapOf("u2" to 5.0)), "me", tappedToReveal = true))
        assertFalse(ScoreReveal.isTmdbRatingHidden(review(mapOf("me" to 5.0)), "me", tappedToReveal = false))
        assertTrue(ScoreReveal.isTmdbRatingHidden(review(emptyMap()), null, tappedToReveal = false))
    }
}
