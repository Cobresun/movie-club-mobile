package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Creates a DataStore for storing session cookies.
 * Platform-specific implementations provide the appropriate file path.
 */
expect fun createSessionCookieDataStore(context: Any? = null): DataStore<Preferences>
