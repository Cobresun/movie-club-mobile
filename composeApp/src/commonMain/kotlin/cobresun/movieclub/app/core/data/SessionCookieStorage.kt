package cobresun.movieclub.app.core.data

import io.ktor.client.plugins.cookies.CookiesStorage

/**
 * Cookie storage that persists cookies across app restarts.
 * Platform-specific implementations use DataStore for persistence.
 */
interface SessionCookieStorage : CookiesStorage {
    /**
     * Clears all stored cookies (used on logout).
     */
    suspend fun clearAll()
}
