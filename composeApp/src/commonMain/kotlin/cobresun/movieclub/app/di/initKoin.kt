package cobresun.movieclub.app.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts Koin once per process, before any UI: Android's `MainApplication.onCreate` and
 * iOS's `iOSApp.init` (via `setUpKoin`). Compose picks up this instance; no `KoinApplication` needed.
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModule, platformModule)
    }
}
