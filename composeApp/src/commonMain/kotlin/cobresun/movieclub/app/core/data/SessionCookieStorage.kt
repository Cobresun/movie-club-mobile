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

    /**
     * Whether an unexpired session cookie is stored: the signed-in hint used at
     * launch, before the server has confirmed the session (web `useSessionHint`).
     */
    suspend fun hasSession(): Boolean
}
