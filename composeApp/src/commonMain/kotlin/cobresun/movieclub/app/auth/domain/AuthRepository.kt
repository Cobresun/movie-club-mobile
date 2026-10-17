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
    /**
     * Signs in with a native Sign in with Apple credential, creating the account
     * on first use. The session cookie is captured like email sign-in.
     */
    suspend fun loginWithApple(credential: AppleCredential): Result<User, DataError.Remote>
    suspend fun getUser(): Result<User, DataError.Remote>

    /**
     * Asks the server for the current session: a [User] when signed in, null when
     * the server says there is none. An Error means it couldn't be asked (offline,
     * timeout, server trouble), which is not the same as being signed out.
     */
    suspend fun getSession(): Result<User?, DataError.Remote>

    /** Whether a session is stored on this device (unverified): the launch-time signed-in hint. */
    suspend fun hasStoredSession(): Boolean

    /**
     * Logs out the current user by clearing session cookies.
     */
    suspend fun logout(): EmptyResult<DataError.Remote>

    /**
     * Resends email verification link to the specified email address.
     */
    suspend fun sendVerificationEmail(email: String): EmptyResult<DataError.Remote>
}
