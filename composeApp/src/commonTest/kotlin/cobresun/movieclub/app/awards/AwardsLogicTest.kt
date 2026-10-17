package cobresun.movieclub.app.awards

import cobresun.movieclub.app.awards.data.dto.AwardDto
import cobresun.movieclub.app.awards.data.dto.ClubAwardsDto
import cobresun.movieclub.app.awards.data.dto.NominationDto
import cobresun.movieclub.app.awards.data.dto.NominationMovieDataDto
import cobresun.movieclub.app.awards.data.mappers.toSeason
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.allRevealed
import cobresun.movieclub.app.awards.domain.categoryTitleError
import cobresun.movieclub.app.awards.domain.hasFinishedNominating
import cobresun.movieclub.app.awards.domain.hasFinishedVoting
import cobresun.movieclub.app.awards.domain.isRankedBy
import cobresun.movieclub.app.awards.domain.isSameCategoryTitle
import cobresun.movieclub.app.awards.domain.membersYetToFinish
import cobresun.movieclub.app.awards.domain.needsRanking
import cobresun.movieclub.app.awards.domain.nominationsBy
import cobresun.movieclub.app.awards.domain.rankingOrderFor
import cobresun.movieclub.app.awards.domain.remainingNominationsFor
import cobresun.movieclub.app.awards.domain.scoreAward
import cobresun.movieclub.app.awards.domain.stepChangeError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AwardsLogicTest {

    private fun nom(id: Int, title: String, ranking: Map<String, Int> = emptyMap(), by: List<String> = listOf("a")) =
        Nomination(id, title, null, null, by, ranking)

    private fun season(step: AwardsStep, vararg categories: AwardCategory) =
        AwardsSeason(2025, step, categories.toList())

    @Test
    fun scoreSumsRanksAndLowestWins() {
        val scored = scoreAward(
            listOf(
                nom(1, "Alien", mapOf("a" to 2, "b" to 3)),
                nom(2, "Brazil", mapOf("a" to 1, "b" to 1)),
                nom(3, "Cars", mapOf("a" to 3, "b" to 2)),
            ),
        )
        assertEquals(listOf("a", "b"), scored.voters)
        assertEquals(listOf(2, 1, 3), scored.results.map { it.nomination.movieId })
        assertEquals(listOf(2, 5, 5), scored.results.map { it.score })
        assertEquals(listOf(1, 2, 2), scored.results.map { it.place })
        assertEquals(listOf(true, false, false), scored.results.map { it.winner })
    }

    @Test
    fun tiesShareTheWinAndKeepNominationOrder() {
        val scored = scoreAward(
            listOf(
                nom(1, "Zodiac", mapOf("a" to 1, "b" to 2)),
                nom(2, "Amelie", mapOf("a" to 2, "b" to 1)),
                nom(3, "Heat", mapOf("a" to 3, "b" to 3)),
            ),
        )
        // Stable sort, like the web: equal scores stay in nomination order.
        assertEquals(listOf("Zodiac", "Amelie", "Heat"), scored.results.map { it.nomination.title })
        assertEquals(listOf(true, true, false), scored.results.map { it.winner })
        assertEquals(listOf(1, 1, 3), scored.results.map { it.place })
    }

    @Test
    fun unrankedNomineeCountsAsThatVotersLastPlace() {
        // "b" voted before "New" was nominated, so it has no rank from them.
        val scored = scoreAward(
            listOf(
                nom(1, "Old", mapOf("a" to 2, "b" to 1)),
                nom(2, "New", mapOf("a" to 1)),
            ),
        )
        // Old: 2 + 1 = 3. New: 1 + last place (2) = 3. A tie, not a win on an empty column.
        assertEquals(listOf(3, 3), scored.results.map { it.score })
        assertTrue(scored.results.all { it.winner })
    }

    @Test
    fun noWinnerUntilSomeoneVotesUnlessThereIsOneNominee() {
        val unvoted = scoreAward(listOf(nom(1, "A"), nom(2, "B")))
        assertTrue(unvoted.voters.isEmpty())
        assertTrue(unvoted.results.none { it.winner })

        val single = scoreAward(listOf(nom(1, "Only")))
        assertTrue(single.results.single().winner)

        assertTrue(scoreAward(emptyList()).results.isEmpty())
    }

    @Test
    fun singleNomineeCategoriesNeedNoRanking() {
        assertFalse(AwardCategory("A", listOf(nom(1, "One"))).needsRanking())
        assertFalse(AwardCategory("B", emptyList()).needsRanking())
        assertTrue(AwardCategory("C", listOf(nom(1, "One"), nom(2, "Two"))).needsRanking())
    }

    @Test
    fun rankingOrderPutsRankedFirstThenUnrankedInNominationOrder() {
        val category = AwardCategory(
            "Editing",
            listOf(
                nom(1, "One", mapOf("me" to 2)),
                nom(2, "Two"),
                nom(3, "Three", mapOf("me" to 1)),
                nom(4, "Four"),
            ),
        )
        assertEquals(listOf(3, 1, 2, 4), category.rankingOrderFor("me").map { it.movieId })
        assertEquals(listOf(1, 2, 3, 4), category.rankingOrderFor("someone").map { it.movieId })
    }

    @Test
    fun nominationsPerMember() {
        val category = AwardCategory(
            "Score",
            listOf(nom(1, "A", by = listOf("me", "you")), nom(2, "B", by = listOf("you"))),
        )
        assertEquals(listOf(1), category.nominationsBy("me").map { it.movieId })
        assertEquals(1, category.remainingNominationsFor("me"))
        assertEquals(0, category.remainingNominationsFor("you"))
        assertEquals(2, category.remainingNominationsFor("new"))
    }

    @Test
    fun finishedVotingSkipsCategoriesThatNeedNoRanking() {
        val ranked = AwardCategory("X", listOf(nom(1, "A", mapOf("a" to 1)), nom(2, "B", mapOf("a" to 2))))
        val single = AwardCategory("Y", listOf(nom(3, "C")))
        val unranked = AwardCategory("Z", listOf(nom(4, "D"), nom(5, "E")))
        assertTrue(ranked.isRankedBy("a"))
        assertFalse(ranked.isRankedBy("b"))
        assertTrue(season(AwardsStep.RANKINGS, ranked, single).hasFinishedVoting("a"))
        assertFalse(season(AwardsStep.RANKINGS, ranked, single, unranked).hasFinishedVoting("a"))
    }

    @Test
    fun finishedNominatingNeedsANomineeInEveryCategoryIncludingBackedOnes() {
        val s = season(
            AwardsStep.NOMINATIONS,
            AwardCategory("X", listOf(nom(1, "A", by = listOf("a", "b")))),
            AwardCategory("Y", listOf(nom(2, "B", by = listOf("a")))),
        )
        assertTrue(s.hasFinishedNominating("a"))
        assertFalse(s.hasFinishedNominating("b"))
        assertEquals(listOf("b", "c"), s.membersYetToFinish(listOf("a", "b", "c")))
    }

    @Test
    fun onlyNominationsAndVotingWaitOnMembers() {
        val category = AwardCategory("X", listOf(nom(1, "A"), nom(2, "B")))
        assertEquals(emptyList(), season(AwardsStep.CATEGORIES, category).membersYetToFinish(listOf("a")))
        assertEquals(listOf("a"), season(AwardsStep.RANKINGS, category).membersYetToFinish(listOf("a")))
        assertEquals(emptyList(), season(AwardsStep.PRESENTATION, category).membersYetToFinish(listOf("a")))
        assertEquals(emptyList(), season(AwardsStep.COMPLETED, category).membersYetToFinish(listOf("a")))
    }

    @Test
    fun stepChangeErrors() {
        val members = listOf("a", "b")
        val empty = season(AwardsStep.CATEGORIES)
        assertEquals("Awards only move forward, one step at a time", empty.stepChangeError(AwardsStep.RANKINGS, members))
        assertEquals("Awards only move forward, one step at a time", empty.stepChangeError(AwardsStep.CATEGORIES, members))
        assertEquals(
            "Add at least one category before opening nominations",
            empty.stepChangeError(AwardsStep.NOMINATIONS, members),
        )
        assertNull(season(AwardsStep.CATEGORIES, AwardCategory("A", emptyList())).stepChangeError(AwardsStep.NOMINATIONS, members))

        val nominating = season(AwardsStep.NOMINATIONS, AwardCategory("A", listOf(nom(1, "One", by = listOf("a")))))
        assertEquals(
            "Everyone has to finish nominating first (1 still to go)",
            nominating.stepChangeError(AwardsStep.RANKINGS, members),
        )
        assertNull(nominating.stepChangeError(AwardsStep.RANKINGS, listOf("a")))

        val voting = season(AwardsStep.RANKINGS, AwardCategory("A", listOf(nom(1, "One"), nom(2, "Two"))))
        assertEquals(
            "Everyone has to finish voting first (2 still to go)",
            voting.stepChangeError(AwardsStep.PRESENTATION, members),
        )
        assertNull(season(AwardsStep.PRESENTATION).stepChangeError(AwardsStep.COMPLETED, members))
    }

    @Test
    fun stepOrder() {
        assertEquals(AwardsStep.NOMINATIONS, AwardsStep.CATEGORIES.manualNext)
        assertEquals(AwardsStep.PRESENTATION, AwardsStep.RANKINGS.manualNext)
        assertNull(AwardsStep.PRESENTATION.manualNext)
        assertEquals(AwardsStep.COMPLETED, AwardsStep.from(4))
        assertEquals(AwardsStep.CATEGORIES, AwardsStep.from(99))
    }

    @Test
    fun allRevealedNeedsEveryCategory() {
        val s = season(AwardsStep.PRESENTATION, AwardCategory("A", emptyList()), AwardCategory("B", emptyList()))
        assertFalse(s.allRevealed(setOf("A")))
        assertTrue(s.allRevealed(setOf("A", "B")))
    }

    @Test
    fun categoryTitleValidation() {
        val s = season(AwardsStep.CATEGORIES, AwardCategory("Best Picture", emptyList()))
        assertEquals("Give the category a name", s.categoryTitleError("   "))
        assertEquals("\"best picture\" is already a category", s.categoryTitleError(" best picture "))
        assertEquals("Keep it under 60 characters", s.categoryTitleError("x".repeat(61)))
        assertNull(s.categoryTitleError("x".repeat(60)))
        assertNull(s.categoryTitleError("Best Picture", except = "Best Picture"))
        assertNull(s.categoryTitleError("Best Score"))
        assertTrue(isSameCategoryTitle(" Best Score", "best score "))
    }

    @Test
    fun mapsServerPayload() {
        val dto = ClubAwardsDto(
            year = 2024,
            step = 2,
            awards = listOf(
                AwardDto(
                    "Best Picture",
                    listOf(
                        NominationDto(
                            movieId = 603,
                            nominatedBy = listOf("u1"),
                            ranking = mapOf("u1" to 1),
                            movieTitle = "The Matrix",
                            posterUrl = "https://image.tmdb.org/t/p/w154/abc.jpg",
                            movieData = NominationMovieDataDto(releaseDate = "1999-03-30"),
                        ),
                        NominationDto(movieId = 7, posterUrl = "https://image.tmdb.org/t/p/w154null"),
                    ),
                ),
            ),
        )
        val season = dto.toSeason()
        assertEquals(AwardsStep.RANKINGS, season.step)
        val (matrix, unknown) = season.categories.single().nominations
        assertEquals("The Matrix", matrix.title)
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", matrix.posterUrl)
        assertEquals(1999, matrix.releaseYear)
        assertNull(unknown.posterUrl)
        assertEquals("Movie #7", unknown.title)
    }
}
