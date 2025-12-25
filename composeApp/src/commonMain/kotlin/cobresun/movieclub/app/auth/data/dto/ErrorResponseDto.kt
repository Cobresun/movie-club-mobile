package cobresun.movieclub.app.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)
