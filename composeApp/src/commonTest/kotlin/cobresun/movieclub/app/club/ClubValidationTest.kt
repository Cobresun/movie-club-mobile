package cobresun.movieclub.app.club

import cobresun.movieclub.app.club.domain.ClubValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClubValidationTest {
    @Test
    fun validSlugsPass() {
        assertNull(ClubValidation.validateSlug("mobile-test-club"))
        assertNull(ClubValidation.validateSlug("abc"))
        assertNull(ClubValidation.validateSlug("a1-b2"))
    }

    @Test
    fun invalidSlugsFail() {
        assertEquals("Link must be 3-50 characters", ClubValidation.validateSlug("ab"))
        assertEquals("Link must be 3-50 characters", ClubValidation.validateSlug("a".repeat(51)))
        assertEquals("Only lowercase letters, numbers, and hyphens allowed", ClubValidation.validateSlug("Abc"))
        assertEquals("Only lowercase letters, numbers, and hyphens allowed", ClubValidation.validateSlug("a_bc"))
        assertEquals("Link cannot be all numbers", ClubValidation.validateSlug("2024"))
        assertNull(ClubValidation.validateSlug("2024-club"))
    }

    @Test
    fun sanitizeSlugInput() {
        assertEquals("my-club2", ClubValidation.sanitizeSlugInput("My Club_2"))
        assertEquals("caf", ClubValidation.sanitizeSlugInput("Café!"))
    }

    @Test
    fun validateName() {
        assertNull(ClubValidation.validateName("Friday Flicks"))
        assertNotNull(ClubValidation.validateName("   "))
        assertNotNull(ClubValidation.validateName("x".repeat(101)))
    }

    @Test
    fun parseInviteTokenFromLinksAndBareTokens() {
        assertEquals("abc123", ClubValidation.parseInviteToken("https://movie-club.app/join-club/abc123"))
        assertEquals("abc123", ClubValidation.parseInviteToken("  https://movie-club.app/join-club/abc123/  "))
        assertEquals("abc123", ClubValidation.parseInviteToken("movieclub://join-club/abc123?x=1"))
        assertEquals("abc123", ClubValidation.parseInviteToken("abc123"))
        assertNull(ClubValidation.parseInviteToken(""))
        assertNull(ClubValidation.parseInviteToken("two words"))
        assertNull(ClubValidation.parseInviteToken("https://movie-club.app/join-club/"))
    }

    @Test
    fun inviteLinkRoundTrips() {
        val link = ClubValidation.inviteLink("tok")
        assertEquals("https://movie-club.app/join-club/tok", link)
        assertEquals("tok", ClubValidation.parseInviteToken(link))
    }
}
