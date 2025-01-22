package cobresun.movieclub.app.member.data.mappers

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.domain.Member

fun MemberDto.toMember(): Member {
    return Member(
        id = id,
        email = email,
        name = name,
        imageUrl = image,
    )
}

fun ClubDto.toClub(): Club {
    return Club(
        id = clubId,
        name = clubName,
    )
}
