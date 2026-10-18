package cobresun.movieclub.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Generous rounding, echoing the web's soft cards. */
val MovieClubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val Base = Typography()

/** Default type scale with heavier display/title weights for a clearer hierarchy. */
val MovieClubTypography = Base.copy(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Whether the app is drawing its dark scheme, for native chrome that needs telling (iOS tab bar, sheets). */
val LocalDarkTheme = staticCompositionLocalOf { true }

/**
 * Movie Club application theme: dark by default (matching the web app), or light.
 * Also points the system chrome (status bar, native views) at the same appearance.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    followSystem: Boolean = false,
    content: @Composable () -> Unit
) {
    SystemChromeAppearance(darkTheme, followSystem)
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) MovieClubDarkColorScheme else MovieClubLightColorScheme,
            shapes = MovieClubShapes,
            typography = MovieClubTypography,
            content = content
        )
    }
}

/**
 * Light or dark status-bar icons (Android) / window appearance (iOS) to match [darkTheme].
 * With [followSystem] iOS clears its override instead: the window's traits are what
 * `isSystemInDarkTheme` reads there, so pinning them would hide the system's setting.
 */
@Composable
internal expect fun SystemChromeAppearance(darkTheme: Boolean, followSystem: Boolean)
