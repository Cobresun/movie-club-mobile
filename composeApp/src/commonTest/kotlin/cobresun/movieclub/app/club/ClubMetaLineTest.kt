package cobresun.movieclub.app.club

import cobresun.movieclub.app.club.presentation.clubMetaLine
import cobresun.movieclub.app.core.domain.ClubType
import kotlin.test.Test
import kotlin.test.assertEquals

class ClubMetaLineTest {
    @Test
    fun degradesToWhicheverHalfHasArrived() {
        assertEquals("", clubMetaLine(null, 4))
        assertEquals("Movie club", clubMetaLine(ClubType.MOVIE, null))
        assertEquals("Book club · 1 member", clubMetaLine(ClubType.BOOK, 1))
        assertEquals("Movie club · 6 members", clubMetaLine(ClubType.MOVIE, 6))
    }
}
