package cobresun.movieclub.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Sets the app's windows to the theme's appearance, so the status bar, the native tab bar
 * and presented sheets (which inherit the window's trait collection) match Compose.
 */
@Composable
internal actual fun SystemChromeAppearance(darkTheme: Boolean, followSystem: Boolean) {
    SideEffect {
        val style = when {
            followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            darkTheme -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.windows?.forEach { window ->
                (window as? UIWindow)?.let { if (it.overrideUserInterfaceStyle != style) it.overrideUserInterfaceStyle = style }
            }
        }
    }
}
