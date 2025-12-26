package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface IdentityDataSource {
    suspend fun login(email: String, password: String): Result<TokenDto, DataError.Remote>

    suspend fun signup(
        email: String,
        password: String,
        fullName: String
    ): Result<TokenDto, DataError.Remote>

    suspend fun getUser(): Result<UserDto, DataError.Remote>

    /**
     * Clears the HTTP client's in-memory bearer token cache.
     * Should be called during logout to prevent automatic re-authentication.
     */
    fun clearTokenCache()
}
