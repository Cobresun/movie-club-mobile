package cobresun.movieclub.app.auth.data.repository

import cobresun.movieclub.app.auth.data.mappers.toUser
import cobresun.movieclub.app.auth.data.network.IdentityDataSource
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.data.BearerTokenStorage
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map
import io.ktor.client.plugins.auth.providers.BearerTokens

class AuthRepositoryImpl(
    private val identityDataSource: IdentityDataSource,
    private val bearerTokenStorage: BearerTokenStorage
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User, DataError.Remote> {
        return identityDataSource.login(email, password)
            .flatMap { tokenDto ->
                bearerTokenStorage.updateToken(
                    BearerTokens(tokenDto.accessToken, tokenDto.refreshToken)
                )

                identityDataSource.getUser()
                    .map { userDto -> userDto.toUser() }
            }
    }

    override suspend fun register(email: String, password: String): Result<User, DataError.Remote> {
        TODO("Not yet implemented")
    }

    override suspend fun getUser(): Result<User, DataError.Remote> {
        return identityDataSource.getUser()
            .map { userDto -> userDto.toUser() }
    }

    override suspend fun logout(): Result<Unit, DataError.Remote> {
        bearerTokenStorage.updateToken(BearerTokens("", ""))
        return Result.Success(Unit)
    }
}
