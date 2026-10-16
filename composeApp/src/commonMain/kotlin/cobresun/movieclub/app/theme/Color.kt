package cobresun.movieclub.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors (match the web app)
val Blue80 = Color(0xFF2196F3)      // Primary
val Blue40 = Color(0xFF1976D2)      // Darker variant
val Blue90 = Color(0xFF64B5F6)      // Lighter variant
val Blue30 = Color(0xFF0D47A1)      // Very dark
val Highlight = Color(0xFF6FBAFF)   // Web "highlight" – Up Next, active accents
val HighlightContainer = Color(0xFF1C3A5A)

// Surface Colors
val DarkGray10 = Color(0xFF0A0C0F)  // Darkest
val DarkGray20 = Color(0xFF1A1D23)  // Lowest container
val DarkGray30 = Color(0xFF222831)  // Web background
val DarkGray35 = Color(0xFF2A303A)  // Low container
val DarkGray40 = Color(0xFF2F343C)  // Container
val DarkGray50 = Color(0xFF393E46)  // Web surface (cards, chips)
val DarkGray55 = Color(0xFF444A53)  // Highest container
val DarkGray60 = Color(0xFF5A6069)  // Outline
val DarkGray80 = Color(0xFFA7ADB5)  // Subdued text
val DarkGray90 = Color(0xFFE0E0E0)  // Very light gray

// Accent Colors
val Gold80 = Color(0xFFFFD54F)      // Star / score accent
val Gold40 = Color(0xFF8A6D00)
val Green80 = Color(0xFF4CAF50)     // Success
val Green40 = Color(0xFF2E7D32)

// Error Colors
val Red80 = Color(0xFFEF5350)
val Red40 = Color(0xFF8E2424)
val Red90 = Color(0xFFFFCDD2)

// Pure Colors
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

/**
 * Movie Club Material 3 Dark Color Scheme.
 *
 * Mirrors the web palette: #222831 background, #393E46 surfaces, #2196F3 primary
 * and #6FBAFF highlight (secondary). Gold (tertiary) is reserved for scores.
 */
val MovieClubDarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = White,
    primaryContainer = Blue40,
    onPrimaryContainer = White,

    secondary = Highlight,
    onSecondary = DarkGray10,
    secondaryContainer = HighlightContainer,
    onSecondaryContainer = Highlight,

    tertiary = Gold80,
    onTertiary = DarkGray10,
    tertiaryContainer = Gold40,
    onTertiaryContainer = White,

    error = Red80,
    onError = DarkGray10,
    errorContainer = Red40,
    onErrorContainer = Red90,

    background = DarkGray30,
    onBackground = White,

    surface = DarkGray30,
    onSurface = White,
    surfaceVariant = DarkGray50,
    onSurfaceVariant = DarkGray80,
    surfaceDim = DarkGray20,
    surfaceBright = DarkGray55,
    surfaceContainerLowest = DarkGray20,
    surfaceContainerLow = DarkGray35,
    surfaceContainer = DarkGray40,
    surfaceContainerHigh = DarkGray50,
    surfaceContainerHighest = DarkGray55,

    outline = DarkGray60,
    outlineVariant = DarkGray50,

    surfaceTint = Blue80,
    inverseSurface = DarkGray90,
    inverseOnSurface = DarkGray20,
    inversePrimary = Blue30,

    scrim = Black
)
