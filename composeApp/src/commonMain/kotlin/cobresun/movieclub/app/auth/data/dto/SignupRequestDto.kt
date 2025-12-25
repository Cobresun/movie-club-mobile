package cobresun.movieclub.app.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequestDto(
    val email: String,
    val password: String,
    val data: SignupDataDto
)

@Serializable
data class SignupDataDto(
    @SerialName("full_name") val fullName: String
)
