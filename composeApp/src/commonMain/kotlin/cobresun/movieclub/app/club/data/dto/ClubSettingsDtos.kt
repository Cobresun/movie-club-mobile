package cobresun.movieclub.app.club.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClubFeaturesDto(
    val awards: Boolean = false,
    val discussionQuestions: Boolean = false,
)

@Serializable
data class ClubSettingsDto(val features: ClubFeaturesDto = ClubFeaturesDto())

/** Partial update: null fields are omitted (explicitNulls = false not assumed, so build by hand). */
@Serializable
data class ClubFeaturesPatchDto(
    val awards: Boolean? = null,
    val discussionQuestions: Boolean? = null,
)

@Serializable
data class ClubSettingsPatchDto(val features: ClubFeaturesPatchDto)

@Serializable
data class NameRequestDto(val name: String)

@Serializable
data class SlugRequestDto(val slug: String)

@Serializable
data class SlugResponseDto(val slug: String)

@Serializable
data class TokenRequestDto(val token: String)

@Serializable
data class InviteInfoDto(
    val clubId: String,
    val clubName: String,
    val slug: String,
    val expiresAt: String? = null,
)

@Serializable
data class ServerErrorDto(val error: String? = null, val message: String? = null)
