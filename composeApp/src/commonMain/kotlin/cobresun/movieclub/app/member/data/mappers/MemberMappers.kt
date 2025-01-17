package cobresun.movieclub.app.member.data.mappers

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.data.dto.ClubDto

fun ClubDto.toClub(): Club {
    return Club(
        id = clubId,
        name = clubName,
    )
}
