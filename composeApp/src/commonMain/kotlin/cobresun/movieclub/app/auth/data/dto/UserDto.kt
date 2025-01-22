package cobresun.movieclub.app.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("user_metadata") val userMetadata: UserMetadataDto,
)

@Serializable
data class UserMetadataDto(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("full_name") val fullName: String,
)
