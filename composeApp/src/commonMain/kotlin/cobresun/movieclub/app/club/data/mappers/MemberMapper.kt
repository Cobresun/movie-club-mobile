package cobresun.movieclub.app.club.data.mappers

import cobresun.movieclub.app.club.data.dto.ClubFeaturesDto
import cobresun.movieclub.app.club.data.dto.InviteInfoDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.club.domain.InviteInfo
import cobresun.movieclub.app.club.domain.MemberRole
import cobresun.movieclub.app.member.domain.Member

fun MemberDto.toDomain(): Member {
    return Member(
        id = id,
        email = email,
        name = name,
        imageUrl = image?.takeIf { it.isNotBlank() },
        role = if (role.equals("admin", ignoreCase = true)) MemberRole.ADMIN else MemberRole.MEMBER,
    )
}

fun ClubFeaturesDto.toDomain() = ClubFeatures(awards = awards, discussionQuestions = discussionQuestions)

fun InviteInfoDto.toDomain() = InviteInfo(clubId = clubId, clubName = clubName, slug = slug)
