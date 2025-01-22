package cobresun.movieclub.app.auth.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface AuthRepository {
    val user: User?
    val accessToken: String?

    suspend fun login(email: String, password: String): Result<User, DataError.Remote>
    suspend fun refreshAccessToken(refreshToken: String): Result<User, DataError.Remote>

    suspend fun register(email: String, password: String): Result<User, DataError.Remote>
}
