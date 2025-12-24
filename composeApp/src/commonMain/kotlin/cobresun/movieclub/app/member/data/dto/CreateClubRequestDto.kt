package cobresun.movieclub.app.member.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateClubRequestDto(
    val name: String,
    val members: List<String>
)
