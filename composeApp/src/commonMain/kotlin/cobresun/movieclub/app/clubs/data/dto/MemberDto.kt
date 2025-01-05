package cobresun.movieclub.app.clubs.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberDto(
    val id: String,
    val email: String,
    val name: String,
    val image: String?,
)
