package cobresun.movieclub.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.sessionCookieDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_cookies"
)

actual fun createSessionCookieDataStore(context: Any?): DataStore<Preferences> {
    require(context is Context) { "Android requires Context for DataStore" }
    return context.sessionCookieDataStore
}
