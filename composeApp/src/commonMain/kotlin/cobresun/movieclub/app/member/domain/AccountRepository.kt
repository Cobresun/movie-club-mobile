package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.Result

/**
 * The signed-in user's own account: profile and credentials.
 * Errors carry the server's message when one is available.
 */
interface AccountRepository {
    suspend fun updateName(name: String): Result<Unit, ApiError>

    /** Uploads [bytes] (an encoded image) as the avatar, replacing any previous one. */
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit, ApiError>

    suspend fun removeAvatar(): Result<Unit, ApiError>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        revokeOtherSessions: Boolean,
    ): Result<Unit, ApiError>

    /** Emails a reset link. Succeeds even for unknown emails (server doesn't reveal accounts). */
    suspend fun requestPasswordReset(email: String): Result<Unit, ApiError>
}
