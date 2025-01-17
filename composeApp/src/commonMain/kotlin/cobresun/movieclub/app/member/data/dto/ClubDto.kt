package cobresun.movieclub.app.member.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClubDto(
    val clubId: String,
    val clubName: String,
)
