package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import cobresun.movieclub.app.proto.BearerTokenData
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BearerTokenStorage(private val dataStore: DataStore<BearerTokenData>) {
    fun getToken(): BearerTokens? {
        return runBlocking { dataStore.data.first() }
            .let {
                if (it.access_token.isEmpty()) {
                    null
                } else {
                    BearerTokens(it.access_token, it.refresh_token)
                }
            }
    }

    fun updateToken(tokens: BearerTokens) {
        runBlocking {
            dataStore.updateData {
                BearerTokenData(
                    access_token = tokens.accessToken,
                    refresh_token = tokens.refreshToken ?: ""
                )
            }
        }
    }
}
