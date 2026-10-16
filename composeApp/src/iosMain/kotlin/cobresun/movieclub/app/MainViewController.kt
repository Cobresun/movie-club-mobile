package cobresun.movieclub.app

import androidx.compose.ui.window.ComposeUIViewController
import cobresun.movieclub.app.app.App
import cobresun.movieclub.app.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) {
    App()
}

/** Called from Swift's `onOpenURL` for movieclub:// (and, once associated domains exist, https) links. */
fun handleDeepLink(url: String): Boolean = cobresun.movieclub.app.app.DeepLinks.handle(url)
