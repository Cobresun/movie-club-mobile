package cobresun.movieclub.app.reviews

import cobresun.movieclub.app.reviews.presentation.encodeUriComponent
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewDetailSectionsTest {
    @Test
    fun encodesTitlesForSearchLinks() {
        assertEquals("Alien", "Alien".encodeUriComponent())
        assertEquals("The%20Thing%20%26%20Co.", "The Thing & Co.".encodeUriComponent())
        assertEquals("Am%C3%A9lie", "Amélie".encodeUriComponent())
    }
}
