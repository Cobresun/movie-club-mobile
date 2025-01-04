package cobresun.movieclub.app

import androidx.compose.ui.window.ComposeUIViewController
import cobresun.movieclub.app.app.App
import cobresun.movieclub.app.di.initKoin

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) {
    App()
}
