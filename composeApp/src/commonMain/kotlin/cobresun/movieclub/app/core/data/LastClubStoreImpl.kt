package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cobresun.movieclub.app.core.domain.LastClubStore
import kotlinx.coroutines.flow.first

/** Stored alongside the session cookies in the app's preferences DataStore. */
class LastClubStoreImpl(
    private val dataStore: DataStore<Preferences>,
) : LastClubStore {
    private val key = stringPreferencesKey("last_club_slug")
    private fun sectionKey(slug: String) = stringPreferencesKey("last_club_section:$slug")

    override suspend fun get(): String? = runCatching { dataStore.data.first()[key] }.getOrNull()

    override suspend fun set(slug: String) {
        runCatching { dataStore.edit { it[key] = slug } }
    }

    override suspend fun getSection(slug: String): String? =
        runCatching { dataStore.data.first()[sectionKey(slug)] }.getOrNull()

    override suspend fun setSection(slug: String, section: String) {
        runCatching { dataStore.edit { it[sectionKey(slug)] = section } }
    }
}
