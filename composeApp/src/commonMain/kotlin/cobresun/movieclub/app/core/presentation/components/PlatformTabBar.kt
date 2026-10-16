package cobresun.movieclub.app.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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
