package cobresun.movieclub.app

import cobresun.movieclub.app.core.domain.ShareLinks
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareLinksTest {
    @Test
    fun matchesWebShareRoutes() {
        assertEquals("https://movie-club.app/share/club/c/review/w1", ShareLinks.review("c", "w1"))
        assertEquals("https://movie-club.app/share/club/c/list/l1", ShareLinks.list("c", "l1"))
        assertEquals("https://movie-club.app/share/club/c/statistics", ShareLinks.statistics("c"))
    }
}
