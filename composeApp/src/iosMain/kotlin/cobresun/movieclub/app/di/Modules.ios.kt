package cobresun.movieclub.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cobresun.movieclub.app.core.data.createSessionCookieDataStore
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.core.platform.HapticFeedback
import io.ktor.client.engine.HttpClientEngine
import cobresun.movieclub.app.core.data.IosNetworkMonitor
import cobresun.movieclub.app.core.data.OfflineStore
import cobresun.movieclub.app.core.data.offlineStoreDirectory
import cobresun.movieclub.app.core.data.NetworkMonitor
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { Darwin.create() }
        single<DataStore<Preferences>> { createSessionCookieDataStore(null) }
        single<NetworkMonitor> { IosNetworkMonitor() }
        single { OfflineStore(offlineStoreDirectory()) }
        single { ClipboardManager() }
        single { HapticFeedback() }
    }
