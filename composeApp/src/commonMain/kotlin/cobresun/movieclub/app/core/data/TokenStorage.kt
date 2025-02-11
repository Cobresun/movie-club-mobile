package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.plugins.auth.providers.BearerTokens

class TokenStorage(private val dataStore: DataStore<Preferences>) {
    // TODO: Temporary, replace with dataStore access
    private val bearerTokenStorage = mutableListOf<BearerTokens>()

    fun getTokens(): List<BearerTokens> {
        return bearerTokenStorage
    }

    fun addTokens(tokens: BearerTokens) {
        bearerTokenStorage.add(tokens)
    }
}
