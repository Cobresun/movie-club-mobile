package cobresun.movieclub.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cobresun.movieclub.app.core.data.createSessionCookieDataStore
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.core.platform.HapticFeedback
import io.ktor.client.engine.HttpClientEngine
import cobresun.movieclub.app.core.data.createResilientOkHttpEngine
import cobresun.movieclub.app.core.data.AndroidNetworkMonitor
import cobresun.movieclub.app.core.data.OfflineStore
import cobresun.movieclub.app.core.data.offlineStoreDirectory
import cobresun.movieclub.app.core.data.NetworkMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<HttpClientEngine> { createResilientOkHttpEngine(androidContext()) }
    single<DataStore<Preferences>> { createSessionCookieDataStore(androidContext()) }
    singleOf(::AndroidNetworkMonitor) { bind<NetworkMonitor>() }
    single { OfflineStore(offlineStoreDirectory(androidContext())) }
    singleOf(::ClipboardManager)
    singleOf(::HapticFeedback)
}
