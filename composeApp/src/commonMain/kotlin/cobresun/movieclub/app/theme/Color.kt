package cobresun.movieclub.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors
val Blue80 = Color(0xFF2196F3)      // Current primary
val Blue40 = Color(0xFF1976D2)      // Darker variant
val Blue90 = Color(0xFF64B5F6)      // Lighter variant
val Blue30 = Color(0xFF0D47A1)      // Very dark

// Surface Colors
val DarkGray10 = Color(0xFF0A0C0F)  // Darkest
val DarkGray20 = Color(0xFF1A1D23)  // Darker than current
val DarkGray30 = Color(0xFF222831)  // Current surface/background
val DarkGray40 = Color(0xFF2D3139)  // Slightly lighter
val DarkGray50 = Color(0xFF393E46)  // Current LIGHT_GRAY
val DarkGray60 = Color(0xFF52575D)  // Medium gray
val DarkGray80 = Color(0xFF9E9E9E)  // Light gray
val DarkGray90 = Color(0xFFE0E0E0)  // Very light gray

// Accent Colors
val Gold80 = Color(0xFFFFD700)      // Current highlight color
val Gold40 = Color(0xFFFFB300)      // Darker gold
val Green80 = Color(0xFF4CAF50)     // Secondary actions
val Green40 = Color(0xFF2E7D32)     // Darker green

// Error Colors
val Red80 = Color(0xFFEF5350)       // Error state
val Red40 = Color(0xFFC62828)       // Darker red
val Red90 = Color(0xFFFFCDD2)       // Light error background

// Pure Colors
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

/**
 * Movie Club Material 3 Dark Color Scheme
 *
 * Based on blue primary (#2196F3) with dark gray surfaces.
 * Uses gold (#FFD700) for secondary/highlight accents.
 */
val MovieClubDarkColorScheme = darkColorScheme(
    // Primary
    primary = Blue80,
    onPrimary = DarkGray10,
    primaryContainer = Blue40,
    onPrimaryContainer = White,

    // Secondary (gold for highlights)
    secondary = Gold80,
    onSecondary = DarkGray10,
    secondaryContainer = Gold40,
    onSecondaryContainer = White,

    // Tertiary (green for success states)
    tertiary = Green80,
    onTertiary = DarkGray10,
    tertiaryContainer = Green40,
    onTertiaryContainer = White,

    // Error
    error = Red80,
    onError = DarkGray10,
    errorContainer = Red40,
    onErrorContainer = Red90,

    // Background
    background = DarkGray20,
    onBackground = White,

    // Surface
    surface = DarkGray30,
    onSurface = White,
    surfaceVariant = DarkGray50,
    onSurfaceVariant = DarkGray90,

    // Outline
    outline = DarkGray60,
    outlineVariant = DarkGray40,

    // Surface Tones
    surfaceTint = Blue80,
    inverseSurface = DarkGray90,
    inverseOnSurface = DarkGray20,
    inversePrimary = Blue30,

    // Scrim
    scrim = Black
)
