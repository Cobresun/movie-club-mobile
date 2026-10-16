package cobresun.movieclub.app.auth.data.dto

import kotlinx.serialization.Serializable

/**
 * Body for BetterAuth `POST /api/auth/sign-in/social` with a provider ID token.
 * Optional fields default to null so they are omitted (BetterAuth rejects explicit nulls).
 */
@Serializable
data class SocialSignInRequestDto(
    val provider: String,
    val idToken: SocialIdTokenDto,
)

@Serializable
data class SocialIdTokenDto(
    val token: String,
    val nonce: String? = null,
    val user: SocialIdTokenUserDto? = null,
)

@Serializable
data class SocialIdTokenUserDto(
    val name: SocialIdTokenNameDto? = null,
)

@Serializable
data class SocialIdTokenNameDto(
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class SocialSignInResponseDto(
    val redirect: Boolean = false,
    val token: String? = null,
    val user: BetterAuthUserDto? = null,
)
