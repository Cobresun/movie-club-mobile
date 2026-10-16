package cobresun.movieclub.app.member.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateClubRequestDto(
    val name: String,
    val members: List<String>,
    val type: String,
)

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
    val revokeOtherSessions: Boolean,
)

@Serializable
data class PasswordResetRequestDto(
    val email: String,
    val redirectTo: String,
)
