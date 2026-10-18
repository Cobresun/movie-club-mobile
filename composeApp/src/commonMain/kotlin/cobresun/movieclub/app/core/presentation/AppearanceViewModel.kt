package cobresun.movieclub.app.core.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AppearanceSettings
import cobresun.movieclub.app.core.domain.ThemeMode
import cobresun.movieclub.app.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(private val settings: AppearanceSettings) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}

/** Whether to draw dark for [mode], following the system for [ThemeMode.SYSTEM]. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** [AppTheme] in the user's chosen appearance. */
@Composable
fun AppearanceTheme(viewModel: AppearanceViewModel, content: @Composable () -> Unit) {
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    AppTheme(darkTheme = mode.isDark(), followSystem = mode == ThemeMode.SYSTEM, content = content)
}
