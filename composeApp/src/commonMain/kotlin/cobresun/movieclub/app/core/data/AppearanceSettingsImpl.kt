package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cobresun.movieclub.app.core.domain.AppearanceSettings
import cobresun.movieclub.app.core.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Stored alongside the session cookies in the app's preferences DataStore. */
class AppearanceSettingsImpl(
    private val dataStore: DataStore<Preferences>,
) : AppearanceSettings {
    private val key = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> prefs[key]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.DARK }
        .catch { emit(ThemeMode.DARK) }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        runCatching { dataStore.edit { it[key] = mode.name } }
    }
}
