package cobresun.movieclub.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cobresun.movieclub.app.core.data.createDataStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { Darwin.create() }
        single<DataStore<Preferences>> { createDataStore() }
    }
