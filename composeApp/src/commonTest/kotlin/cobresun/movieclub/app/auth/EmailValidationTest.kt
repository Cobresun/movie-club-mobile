package cobresun.movieclub.app.auth

import cobresun.movieclub.app.auth.presentation.isPlausibleEmail
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailValidationTest {
    @Test
    fun acceptsNormalAddresses() {
        assertTrue(isPlausibleEmail("a@b.co"))
        assertTrue(isPlausibleEmail("  first.last+tag@example.com "))
    }

    @Test
    fun rejectsMalformedAddresses() {
        listOf("", "notanemail", "a@b", "@b.com", "a b@c.com", "a@@b.com").forEach {
            assertFalse(isPlausibleEmail(it), it)
        }
    }
}
