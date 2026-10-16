package cobresun.movieclub.app.member.data.mappers

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.data.mappers.toDomain
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.domain.Member

fun MemberDto.toMember(): Member = toDomain()

fun ClubDto.toClub(): Club {
    return Club(
        id = clubId,
        slug = slug,
        name = clubName,
        type = ClubType.from(type),
    )
}
