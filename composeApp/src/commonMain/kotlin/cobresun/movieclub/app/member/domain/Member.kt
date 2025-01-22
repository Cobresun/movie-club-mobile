package cobresun.movieclub.app.member.domain

data class Member(
    val id: String,
    val email: String,
    val name: String,
    val imageUrl: String? = null,
)
