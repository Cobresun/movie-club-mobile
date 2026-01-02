package cobresun.movieclub.app.auth.data.repository

import cobresun.movieclub.app.auth.data.mappers.toUser
import cobresun.movieclub.app.auth.data.network.BetterAuthDataSource
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.data.SessionCookieStorage
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map

class AuthRepositoryImpl(
    private val betterAuthDataSource: BetterAuthDataSource,
    private val cookieStorage: SessionCookieStorage
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User, DataError.Remote> {
        return betterAuthDataSource.signIn(email, password)
            .map { userDto -> userDto.toUser() }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<User, DataError.Remote> {
        return betterAuthDataSource.signUp(email, password, fullName)
            .map { userDto -> userDto.toUser() }
    }

    override suspend fun getUser(): Result<User, DataError.Remote> {
        return when (val result = betterAuthDataSource.getSession()) {
            is Result.Success -> {
                val userDto = result.data
                if (userDto != null) {
                    Result.Success(userDto.toUser())
                } else {
                    // No active session - not an error, just not authenticated
                    Result.Error(DataError.Remote.UNKNOWN)
                }
            }
            is Result.Error -> result
        }
    }

    override suspend fun logout(): Result<Unit, DataError.Remote> {
        // Sign out on server (clears server-side session)
        val result = betterAuthDataSource.signOut()

        // Clear local cookie storage
        cookieStorage.clearAll()

        return result
    }

    override suspend fun sendVerificationEmail(email: String): Result<Unit, DataError.Remote> {
        return betterAuthDataSource.sendVerificationEmail(email)
    }
}
