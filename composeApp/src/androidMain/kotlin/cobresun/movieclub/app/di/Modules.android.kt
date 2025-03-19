package cobresun.movieclub.app.di

import androidx.datastore.core.DataStore
import cobresun.movieclub.app.core.data.createBearerTokenDataStore
import cobresun.movieclub.app.proto.BearerTokenData
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<HttpClientEngine> { OkHttp.create() }
        single<DataStore<BearerTokenData>> { createBearerTokenDataStore(androidContext()) }
    }
