package cobresun.movieclub.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cobresun.movieclub.app.core.data.createSessionCookieDataStore
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.core.platform.HapticFeedback
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { OkHttp.create() }
        single<DataStore<Preferences>> { createSessionCookieDataStore(androidContext()) }
        single { ClipboardManager(androidContext()) }
        single { HapticFeedback(androidContext()) }
    }
