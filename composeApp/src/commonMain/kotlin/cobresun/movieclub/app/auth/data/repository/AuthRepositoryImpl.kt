package cobresun.movieclub.app.auth.data.repository

import cobresun.movieclub.app.auth.data.mappers.toSocialSignInRequest
import cobresun.movieclub.app.auth.data.mappers.toUser
import cobresun.movieclub.app.auth.data.network.BetterAuthDataSource
import cobresun.movieclub.app.auth.domain.AppleCredential
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.domain.User
import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.SessionCookieStorage
import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.core.domain.onSuccess

class AuthRepositoryImpl(
    private val betterAuthDataSource: BetterAuthDataSource,
    private val cookieStorage: SessionCookieStorage,
    private val caches: SessionCaches,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User, ApiError> {
        return betterAuthDataSource.signIn(email, password)
            .map { userDto -> userDto.toUser() }
            .onSuccess { caches.clear() }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String,
        callbackURL: String,
    ): Result<User, ApiError> {
        return betterAuthDataSource.signUp(email, password, fullName, callbackURL)
            .map { userDto -> userDto.toUser() }
    }

    override suspend fun loginWithApple(credential: AppleCredential): Result<User, DataError.Remote> {
        return betterAuthDataSource.signInWithIdToken(credential.toSocialSignInRequest())
            .map { userDto -> userDto.toUser() }
            .onSuccess { caches.clear() }
    }

    override suspend fun getSession(): Result<User?, DataError.Remote> =
        betterAuthDataSource.getSession().map { it?.toUser() }

    override suspend fun hasStoredSession(): Boolean = cookieStorage.hasSession()

    override suspend fun logout(): Result<Unit, DataError.Remote> {
        // Sign out on server (clears server-side session)
        val result = betterAuthDataSource.signOut()

        // Clear local cookie storage and everything read as this user
        cookieStorage.clearAll()
        caches.clear()

        return result
    }

    override suspend fun sendVerificationEmail(email: String, callbackURL: String): Result<Unit, DataError.Remote> {
        return betterAuthDataSource.sendVerificationEmail(email, callbackURL)
    }
}
