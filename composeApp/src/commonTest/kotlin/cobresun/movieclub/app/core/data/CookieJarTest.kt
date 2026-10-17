package cobresun.movieclub.app.core.data

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import io.ktor.util.date.GMTDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookieJarTest {
    private val api = Url("https://movie-club.app/api/auth/get-session")
    private val now = 1_800_000_000_000L

    @Test
    fun maxAgeBecomesAnAbsoluteExpiry() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "1", maxAge = 60), api, now)
        assertEquals(now + 60_000, stored.single().expires?.timestamp)
        assertEquals(1, CookieJar.select(stored, api, now + 59_000).size)
        assertTrue(CookieJar.select(stored, api, now + 61_000).isEmpty())
    }

    @Test
    fun expiryOutlivesARoundTripThroughStorage() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "1", maxAge = 60), api, now)
        val reread = stored.map { parseServerSetCookieHeader(renderSetCookieHeader(it)) }
        // Re-reading much later must not restart a relative Max-Age.
        assertTrue(CookieJar.select(reread, api, now + 120_000).isEmpty())
    }

    @Test
    fun maxAgeZeroDeletesTheStoredCookie() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "1"), api, now)
        val cleared = CookieJar.add(stored, Cookie("s", "", maxAge = 0), api, now)
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun pastExpiresDeletesTheStoredCookie() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "1"), api, now)
        val cleared = CookieJar.add(stored, Cookie("s", "", expires = GMTDate(now - 1)), api, now)
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun aRefreshedCookieReplacesTheOldOne() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "old", maxAge = 60), api, now)
        val refreshed = CookieJar.add(stored, Cookie("s", "new", maxAge = 3600), api, now + 30_000)
        assertEquals(listOf("new"), CookieJar.select(refreshed, api, now + 120_000).map { it.value })
    }

    @Test
    fun cookiesOnlyGoBackToTheirOwnHost() {
        val stored = CookieJar.add(emptyList(), Cookie("s", "1", secure = true), api, now)
        assertTrue(CookieJar.select(stored, Url("https://api.themoviedb.org/3/search"), now).isEmpty())
        assertTrue(CookieJar.select(stored, Url("http://movie-club.app/api"), now).isEmpty())
        assertEquals(1, CookieJar.select(stored, Url("https://movie-club.app/api/club"), now).size)
    }

    @Test
    fun legacyCookiesWithoutADomainStillReachTheApiAndAreReplaced() {
        val legacy = listOf(Cookie("s", "old"))
        assertEquals(1, CookieJar.select(legacy, api, now).size)
        val updated = CookieJar.add(legacy, Cookie("s", "new"), api, now)
        assertEquals(listOf("new"), updated.map { it.value })
    }
}
