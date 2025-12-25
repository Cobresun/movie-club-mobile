package cobresun.movieclub.app.club.data.mappers

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.member.domain.Member

fun MemberDto.toDomain(): Member {
    return Member(
        id = id,
        email = email,
        name = name,
        imageUrl = image
    )
}
