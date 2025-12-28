package cobresun.movieclub.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Movie Club application theme.
 *
 * Currently uses dark theme only. Light theme support can be added in the future
 * by implementing MovieClubLightColorScheme and adding a darkTheme parameter.
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MovieClubDarkColorScheme,
        content = content
    )
}
