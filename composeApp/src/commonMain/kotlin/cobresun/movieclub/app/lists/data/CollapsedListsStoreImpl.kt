package cobresun.movieclub.app.lists.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
import kotlinx.coroutines.flow.first

/** Stored in the app's preferences DataStore; storage failures just drop the state, like web. */
class CollapsedListsStoreImpl(
    private val dataStore: DataStore<Preferences>,
) : CollapsedListsStore {
    private fun key(clubSlug: String) = stringSetPreferencesKey("collapsed_lists:$clubSlug")

    override suspend fun get(clubSlug: String): Set<String> =
        runCatching { dataStore.data.first()[key(clubSlug)] }.getOrNull().orEmpty()

    override suspend fun set(clubSlug: String, listIds: Set<String>) {
        runCatching { dataStore.edit { it[key(clubSlug)] = listIds } }
    }
}
