package cobresun.movieclub.app

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import cobresun.movieclub.app.app.App
import cobresun.movieclub.app.di.initKoin

/** Called once from Swift's `iOSApp.init`, before any view controller exists. */
fun setUpKoin() = initKoin()

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Layouts make room for the keyboard with ime insets, as on Android, rather
        // than iOS panning the whole UI (which pushed sheets under the status bar).
        onFocusBehavior = OnFocusBehavior.DoNothing
    }
) {
    App()
}

/** Called from Swift's `onOpenURL` for movieclub:// (and, once associated domains exist, https) links. */
fun handleDeepLink(url: String): Boolean = cobresun.movieclub.app.app.DeepLinks.handle(url)
