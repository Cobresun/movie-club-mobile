package cobresun.movieclub.app.club

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.defaultClubSlug
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultClubSlugTest {
    private val clubs = listOf(Club("1", "movies", "Movies"), Club("2", "books", "Books"))

    @Test
    fun opensRememberedClub() = assertEquals("books", defaultClubSlug(clubs, "books"))

    @Test
    fun fallsBackToFirstWhenNothingRemembered() = assertEquals("movies", defaultClubSlug(clubs, null))

    @Test
    fun fallsBackToFirstWhenRememberedClubWasLeft() = assertEquals("movies", defaultClubSlug(clubs, "gone"))

    @Test
    fun noClubs() = assertNull(defaultClubSlug(emptyList(), "books"))
}
