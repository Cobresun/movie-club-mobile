package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.BetterAuthUserDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface BetterAuthDataSource {
    suspend fun signUp(email: String, password: String, name: String): Result<BetterAuthUserDto, DataError.Remote>
    suspend fun signIn(email: String, password: String): Result<BetterAuthUserDto, DataError.Remote>
    suspend fun signOut(): Result<Unit, DataError.Remote>
    suspend fun getSession(): Result<BetterAuthUserDto?, DataError.Remote>
    suspend fun sendVerificationEmail(email: String): Result<Unit, DataError.Remote>
}
