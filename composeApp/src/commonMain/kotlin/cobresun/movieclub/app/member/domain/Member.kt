package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.club.domain.MemberRole

data class Member(
    val id: String,
    val email: String,
    val name: String,
    val imageUrl: String? = null,
    val role: MemberRole = MemberRole.MEMBER,
)
