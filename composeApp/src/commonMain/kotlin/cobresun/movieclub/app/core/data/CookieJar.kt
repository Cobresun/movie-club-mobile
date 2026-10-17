package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate

/**
 * The cookie rules [SessionCookieStorageImpl] applies, kept pure so they can be
 * tested: expiry (`Expires` / `Max-Age`, including `Max-Age=0` deletion), host
 * matching, and replacing a cookie by name.
 */
internal object CookieJar {
    private val LEGACY_HOST = Url(BASE_URL).host

    /**
     * Stores [incoming] (from a response to [requestUrl]) into [stored]. `Max-Age`
     * becomes an absolute expiry, since the stored copy is read back later; a cookie
     * that is already expired deletes any stored one of the same name.
     */
    fun add(stored: List<Cookie>, incoming: Cookie, requestUrl: Url, nowMillis: Long): List<Cookie> {
        val cookie = incoming.withDefaults(requestUrl, nowMillis)
        val kept = stored.filterNot { it.name == cookie.name && (it.domain ?: LEGACY_HOST) == cookie.domain }
            .filterNot { it.expiredAt(nowMillis) }
        return if (cookie.expiredAt(nowMillis)) kept else kept + cookie
    }

    /** The cookies to send to [requestUrl]: unexpired, for its host, and secure ones only over https. */
    fun select(stored: List<Cookie>, requestUrl: Url, nowMillis: Long): List<Cookie> =
        stored.filter { cookie ->
            !cookie.expiredAt(nowMillis) &&
                cookie.matchesHost(requestUrl.host) &&
                (!cookie.secure || requestUrl.protocol.name == "https")
        }

    fun isExpired(cookie: Cookie, nowMillis: Long): Boolean = cookie.expiredAt(nowMillis)

    private fun Cookie.expiredAt(nowMillis: Long): Boolean =
        maxAge == 0 || (expires?.let { it.timestamp <= nowMillis } ?: false)

    private fun Cookie.withDefaults(requestUrl: Url, nowMillis: Long): Cookie {
        val absoluteExpiry = maxAge?.let { age -> GMTDate(nowMillis + age * 1000L) } ?: expires
        return copy(
            domain = (domain ?: requestUrl.host).trimStart('.').lowercase(),
            path = path ?: "/",
            maxAge = if (maxAge == 0) 0 else null,
            expires = absoluteExpiry,
        )
    }

    private fun Cookie.matchesHost(host: String): Boolean {
        // Cookies saved before domains were recorded all came from the app's own API.
        val domain = domain ?: LEGACY_HOST
        val h = host.lowercase()
        return h == domain || h.endsWith(".$domain")
    }
}
