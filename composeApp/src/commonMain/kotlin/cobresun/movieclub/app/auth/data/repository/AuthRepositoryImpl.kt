package cobresun.movieclub.app.auth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cobresun.movieclub.app.auth.data.mappers.toUser
import cobresun.movieclub.app.auth.data.network.IdentityDataSource
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val identityDataSource: IdentityDataSource,
    private val prefs: DataStore<Preferences>,
) : AuthRepository {
    override val userAccessToken = prefs.data.map { it[stringPreferencesKey("access_token")] }

    override suspend fun login(email: String, password: String): Result<User, DataError.Remote> {
        return identityDataSource.login(email, password)
            .flatMap { tokenDto ->
                identityDataSource.getUser(tokenDto.accessToken)
                    .map { userDto ->
                        val user = userDto.toUser(tokenDto)
                        prefs.edit {
                            it[stringPreferencesKey("access_token")] = user.token.accessToken
                        }
                        user
                    }
            }
    }

    override suspend fun refreshAccessToken(refreshToken: String): Result<User, DataError.Remote> {
        return identityDataSource.refreshAccessToken(refreshToken)
            .flatMap { tokenDto ->
                identityDataSource.getUser(tokenDto.accessToken)
                    .map { userDto ->
                        val user = userDto.toUser(tokenDto)
                        prefs.edit {
                            it[stringPreferencesKey("access_token")] = user.token.accessToken
                        }
                        user
                    }
            }
    }

    override suspend fun register(email: String, password: String): Result<User, DataError.Remote> {
        TODO("Not yet implemented")
    }
}
