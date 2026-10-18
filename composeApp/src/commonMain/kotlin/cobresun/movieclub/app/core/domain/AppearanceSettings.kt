package cobresun.movieclub.app.core.domain

import kotlinx.coroutines.flow.Flow

/** Which color scheme the app draws with. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The user's appearance choice, kept on the device. Dark by default, like the web app. */
interface AppearanceSettings {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
