package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.http.renderSetCookieHeader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SessionCookieStorageImpl(
    private val dataStore: DataStore<Preferences>
) : SessionCookieStorage {

    private val cookieKey = stringPreferencesKey("session_cookies")

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val cookiesString = dataStore.data.map { prefs ->
            prefs[cookieKey] ?: ""
        }.first()

        if (cookiesString.isEmpty()) return emptyList()

        // Parse stored cookies (separated by ||)
        return cookiesString.split(COOKIE_SEPARATOR)
            .filter { it.isNotEmpty() }
            .mapNotNull { cookieString ->
                try {
                    parseServerSetCookieHeader(cookieString)
                } catch (e: Exception) {
                    null // Skip invalid cookies
                }
            }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        dataStore.edit { prefs ->
            val existing = prefs[cookieKey]
                ?.split(COOKIE_SEPARATOR)
                ?.toMutableList()
                ?: mutableListOf()

            // Remove existing cookie with same name
            existing.removeAll { it.startsWith("${cookie.name}=") }

            // Add new cookie
            existing.add(renderSetCookieHeader(cookie))

            prefs[cookieKey] = existing.joinToString(COOKIE_SEPARATOR)
        }
    }

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
    }
}
