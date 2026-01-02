package cobresun.movieclub.app.auth.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.EmptyResult
import cobresun.movieclub.app.core.domain.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User, DataError.Remote>
    suspend fun register(
        email: String,
        password: String,
        fullName: String
    ): Result<User, DataError.Remote>
    suspend fun getUser(): Result<User, DataError.Remote>

    /**
     * Logs out the current user by clearing session cookies.
     */
    suspend fun logout(): EmptyResult<DataError.Remote>

    /**
     * Resends email verification link to the specified email address.
     */
    suspend fun sendVerificationEmail(email: String): EmptyResult<DataError.Remote>
}
