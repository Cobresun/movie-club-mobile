package cobresun.movieclub.app

import cobresun.movieclub.app.app.DeepLink
import cobresun.movieclub.app.app.DeepLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinksTest {
    @Test
    fun parsesHttpsInvite() {
        assertEquals(DeepLink.JoinClub("abc123"), DeepLinks.parse("https://movie-club.app/join-club/abc123"))
        assertEquals(DeepLink.JoinClub("abc123"), DeepLinks.parse("https://www.movie-club.app/join-club/abc123/?x=1"))
    }

    @Test
    fun parsesCustomSchemeInvite() {
        assertEquals(DeepLink.JoinClub("tok"), DeepLinks.parse("movieclub://join-club/tok"))
        assertEquals(DeepLink.JoinClub("tok"), DeepLinks.parse("movieclub:///join-club/tok#frag"))
    }

    @Test
    fun rejectsOtherLinks() {
        assertNull(DeepLinks.parse("https://evil.com/join-club/abc"))
        assertNull(DeepLinks.parse("https://movie-club.app/club/foo"))
        assertNull(DeepLinks.parse("https://movie-club.app/join-club/"))
        assertNull(DeepLinks.parse("ftp://movie-club.app/join-club/a"))
        assertNull(DeepLinks.parse("not a url"))
    }

    @Test
    fun handleStoresUntilConsumed() {
        DeepLinks.handle("movieclub://join-club/xyz")
        val link = DeepLinks.pending.value
        assertEquals(DeepLink.JoinClub("xyz"), link)
        DeepLinks.consume(link!!)
        assertNull(DeepLinks.pending.value)
    }
}
