package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.flow.first

/**
 * Persists cookies (the Better Auth session) in DataStore so a session survives app
 * restarts. Every response's `Set-Cookie` lands here through Ktor's HttpCookies
 * plugin, including the refreshed session cookie `get-session` sends back.
 */
class SessionCookieStorageImpl(
    private val dataStore: DataStore<Preferences>,
    private val now: () -> Long = ::getTimeMillis,
) : SessionCookieStorage {

    private val cookieKey = stringPreferencesKey("session_cookies")

    private fun decode(raw: String?): List<Cookie> =
        raw.orEmpty().split(COOKIE_SEPARATOR)
            .filter { it.isNotEmpty() }
            .mapNotNull { runCatching { parseServerSetCookieHeader(it) }.getOrNull() }

    private fun encode(cookies: List<Cookie>): String = cookies.joinToString(COOKIE_SEPARATOR) { renderSetCookieHeader(it) }

    private suspend fun stored(): List<Cookie> = runCatching { decode(dataStore.data.first()[cookieKey]) }.getOrDefault(emptyList())

    override suspend fun get(requestUrl: Url): List<Cookie> = CookieJar.select(stored(), requestUrl, now())

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        dataStore.edit { prefs ->
            val updated = CookieJar.add(decode(prefs[cookieKey]), cookie, requestUrl, now())
            if (updated.isEmpty()) prefs.remove(cookieKey) else prefs[cookieKey] = encode(updated)
        }
    }

    override suspend fun hasSession(): Boolean =
        stored().any { it.name.contains(SESSION_COOKIE) && !CookieJar.isExpired(it, now()) }

    override fun close() {
        // DataStore handles cleanup
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(cookieKey)
        }
    }

    companion object {
        private const val COOKIE_SEPARATOR = "||"

        /** Better Auth's session cookie (`better-auth.session_token`, `__Secure-` prefixed over https). */
        private const val SESSION_COOKIE = "session_token"
    }
}
