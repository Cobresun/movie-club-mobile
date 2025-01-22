package cobresun.movieclub.app.auth.data.repository

import cobresun.movieclub.app.auth.data.mappers.toUser
import cobresun.movieclub.app.auth.data.network.IdentityDataSource
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map

class AuthRepositoryImpl(
    private val identityDataSource: IdentityDataSource
) : AuthRepository {
    // TODO: Store user and token info in DataStore
    private var inMemoryUser: User? = null
    override val user: User?
        get() = inMemoryUser

    private var inMemoryAccessToken: String? = null
    override val accessToken: String?
        get() = inMemoryAccessToken

    override suspend fun login(email: String, password: String): Result<User, DataError.Remote> {
        return identityDataSource.login(email, password)
            .map { tokenDto ->
                inMemoryAccessToken = tokenDto.accessToken
                tokenDto.accessToken
            }
            .flatMap { accessToken ->
                identityDataSource.getUser(accessToken)
                    .map { userDto ->
                        val user = userDto.toUser()
                        inMemoryUser = user
                        user
                    }
            }
    }

    override suspend fun refreshAccessToken(refreshToken: String): Result<User, DataError.Remote> {
        return identityDataSource.refreshAccessToken(refreshToken)
            .map { tokenDto ->
                inMemoryAccessToken = tokenDto.accessToken
                tokenDto.accessToken
            }
            .flatMap { accessToken ->
                identityDataSource.getUser(accessToken)
                    .map { userDto ->
                        val user = userDto.toUser()
                        inMemoryUser = user
                        user
                    }
            }
    }

    override suspend fun register(email: String, password: String): Result<User, DataError.Remote> {
        TODO("Not yet implemented")
    }
}
