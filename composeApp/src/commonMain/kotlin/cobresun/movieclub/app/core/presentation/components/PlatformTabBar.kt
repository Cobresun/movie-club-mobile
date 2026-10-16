package cobresun.movieclub.app.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * One destination in a [PlatformTabBar].
 *
 * [iosSymbol]/[iosSelectedSymbol] are SF Symbol names used by the native iOS tab bar;
 * [icon]/[selectedIcon] are used by the Material bar on Android.
 */
data class PlatformTab(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val iosSymbol: String,
    val iosSelectedSymbol: String,
)

/**
 * The app's bottom tab bar, rendered natively per platform:
 * - Android: Material 3 navigation bar in the brand palette.
 * - iOS: a real UIKit `UITabBar` layered over Compose, so on iOS 26 it gets
 *   Liquid Glass (and SF Symbols, native haptics/accessibility) for free.
 */
@Composable
expect fun PlatformTabBar(
    tabs: List<PlatformTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * False while something drawn by Compose (e.g. the club drawer) covers the screen.
 * Native views layered over Compose (the iOS tab bar) hide themselves then, since
 * they would otherwise draw on top of it.
 */
val LocalNativeChromeVisible = compositionLocalOf { true }

/**
 * True where the tab bar is translucent and content should scroll underneath it
 * (the iOS Liquid Glass `UITabBar`). False on Android, where the Material bar is opaque
 * and content is simply laid out above it.
 */
expect val tabBarOverlaysContent: Boolean

/**
 * Height of the tab bar that content scrolls beneath, or 0.dp where it doesn't overlay.
 * Tab screens add it to their scrollable content padding and bottom-anchored controls (FABs).
 */
val LocalTabBarInset = compositionLocalOf { 0.dp }
