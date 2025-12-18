package cobresun.movieclub.app.auth.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.EmptyResult
import cobresun.movieclub.app.core.domain.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User, DataError.Remote>
    suspend fun register(email: String, password: String): Result<User, DataError.Remote>
    suspend fun getUser(): Result<User, DataError.Remote>

    /**
     * Logs out the current user by clearing authentication tokens.
     * This is a local operation that clears stored bearer tokens.
     */
    suspend fun logout(): EmptyResult<DataError.Remote>
}
