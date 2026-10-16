package cobresun.movieclub.app.awards

import cobresun.movieclub.app.awards.data.dto.AwardDto
import cobresun.movieclub.app.awards.data.dto.ClubAwardsDto
import cobresun.movieclub.app.awards.data.dto.NominationDto
import cobresun.movieclub.app.awards.data.dto.NominationMovieDataDto
import cobresun.movieclub.app.awards.data.mappers.toSeason
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.CategoryTitleError
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.allRevealed
import cobresun.movieclub.app.awards.domain.canAdvance
import cobresun.movieclub.app.awards.domain.isRankingComplete
import cobresun.movieclub.app.awards.domain.nominationsBy
import cobresun.movieclub.app.awards.domain.rankingOrderFor
import cobresun.movieclub.app.awards.domain.remainingNominationsFor
import cobresun.movieclub.app.awards.domain.standings
import cobresun.movieclub.app.awards.domain.validateCategoryTitle
import cobresun.movieclub.app.awards.domain.votersDone
import cobresun.movieclub.app.awards.domain.winners
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
    fun standingsSumRanksLowestWins() {
        val category = AwardCategory(
            "Best Picture",
            listOf(
                nom(1, "Alien", mapOf("a" to 2, "b" to 3)),
                nom(2, "Brazil", mapOf("a" to 1, "b" to 1)),
                nom(3, "Cars", mapOf("a" to 3, "b" to 2)),
            ),
        )
        val standings = category.standings()
        assertEquals(listOf(2, 1, 3), standings.map { it.nomination.movieId })
        assertEquals(listOf(2, 5, 5), standings.map { it.points })
        assertEquals(listOf(1, 2, 2), standings.map { it.place })
        assertEquals(listOf("Brazil"), category.winners().map { it.title })
    }

    @Test
    fun tiedWinnersShareFirstPlaceAndNextPlaceIsSkipped() {
        val category = AwardCategory(
            "Score",
            listOf(
                nom(1, "Zodiac", mapOf("a" to 1, "b" to 2)),
                nom(2, "Amelie", mapOf("a" to 2, "b" to 1)),
                nom(3, "Heat", mapOf("a" to 3, "b" to 3)),
            ),
        )
        val standings = category.standings()
        assertEquals(listOf("Amelie", "Zodiac", "Heat"), standings.map { it.nomination.title })
        assertEquals(listOf(1, 1, 3), standings.map { it.place })
        assertEquals(2, category.winners().size)
    }

    @Test
    fun emptyCategoryHasNoWinners() {
        assertTrue(AwardCategory("Empty", emptyList()).winners().isEmpty())
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
    fun rankingCompletionRequiresEveryMemberOnEveryNomination() {
        val members = listOf("a", "b")
        val partial = AwardCategory("X", listOf(nom(1, "A", mapOf("a" to 1, "b" to 1)), nom(2, "B", mapOf("a" to 2))))
        val full = AwardCategory("Y", listOf(nom(3, "C", mapOf("a" to 1, "b" to 1))))
        assertEquals(1, partial.votersDone(members))
        assertFalse(season(AwardsStep.RANKINGS, partial, full).isRankingComplete(members))
        assertTrue(season(AwardsStep.RANKINGS, full).isRankingComplete(members))
        assertFalse(season(AwardsStep.RANKINGS, partial).canAdvance(members))
        assertTrue(season(AwardsStep.RANKINGS, full).canAdvance(members))
    }

    @Test
    fun stepGating() {
        assertFalse(season(AwardsStep.CATEGORIES).canAdvance(emptyList()))
        assertTrue(season(AwardsStep.CATEGORIES, AwardCategory("A", emptyList())).canAdvance(emptyList()))
        assertTrue(season(AwardsStep.NOMINATIONS).canAdvance(emptyList()))
        assertFalse(season(AwardsStep.PRESENTATION).canAdvance(emptyList()))
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
        assertEquals(CategoryTitleError.BLANK, s.validateCategoryTitle("   "))
        assertEquals(CategoryTitleError.DUPLICATE, s.validateCategoryTitle(" best picture "))
        assertNull(s.validateCategoryTitle("Best Picture", except = "Best Picture"))
        assertNull(s.validateCategoryTitle("Best Score"))
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
