package cobresun.movieclub.app

import cobresun.movieclub.app.app.ClubSection
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
        assertNull(DeepLinks.parse("https://movie-club.app/share/club/foo/review"))
        assertNull(DeepLinks.parse("https://movie-club.app/share/club/foo/unknown"))
        assertNull(DeepLinks.parse("https://movie-club.app/join-club/"))
        assertNull(DeepLinks.parse("ftp://movie-club.app/join-club/a"))
        assertNull(DeepLinks.parse("not a url"))
    }

    @Test
    fun parsesSharedReviewLinks() {
        val expected = DeepLink.Club("mobile-test-club", ClubSection.REVIEWS, reviewWorkId = "w1")
        assertEquals(expected, DeepLinks.parse("https://movie-club.app/share/club/mobile-test-club/review/w1"))
        assertEquals(expected, DeepLinks.parse("movieclub://share/club/mobile-test-club/review/w1"))
        assertEquals(DeepLink.Club("c", ClubSection.STATISTICS), DeepLinks.parse("https://movie-club.app/share/club/c/statistics"))
        assertEquals(DeepLink.Club("c", ClubSection.LISTS), DeepLinks.parse("https://movie-club.app/share/club/c/list/l1"))
    }

    @Test
    fun parsesClubSectionLinks() {
        assertEquals(DeepLink.Club("c"), DeepLinks.parse("https://movie-club.app/club/c"))
        assertEquals(DeepLink.Club("c", ClubSection.REVIEWS), DeepLinks.parse("https://movie-club.app/club/c/reviews"))
        assertEquals(DeepLink.Club("c", ClubSection.LISTS), DeepLinks.parse("https://movie-club.app/club/c/watch-list"))
        assertEquals(DeepLink.Club("c", ClubSection.AWARDS), DeepLinks.parse("https://movie-club.app/club/c/awards/2025"))
        assertEquals(DeepLink.Club("c", ClubSection.SETTINGS), DeepLinks.parse("movieclub://club/c/settings"))
        assertEquals(DeepLink.Club("c"), DeepLinks.parse("https://movie-club.app/club/c/wrapped"))
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
