package cobresun.movieclub.app.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BetterAuthUserDto(
    val id: String,
    val email: String,
    val name: String,
    val image: String? = null,
    val emailVerified: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class BetterAuthSessionDto(
    val token: String,
    val expiresAt: String,
    val userId: String
)

@Serializable
data class SignUpResponseDto(
    val user: BetterAuthUserDto,
    val session: BetterAuthSessionDto? = null
)

@Serializable
data class SignInResponseDto(
    val user: BetterAuthUserDto,
    val session: BetterAuthSessionDto? = null
)

@Serializable
data class GetSessionResponseDto(
    val user: BetterAuthUserDto? = null,
    val session: BetterAuthSessionDto? = null
)

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String,
    /** Where the verification email's link lands once verified (web AuthModal). */
    val callbackURL: String? = null,
)

@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class SendVerificationEmailRequestDto(
    val email: String,
    val callbackURL: String
)
