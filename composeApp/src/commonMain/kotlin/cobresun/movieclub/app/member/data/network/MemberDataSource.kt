package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ClubDto

interface MemberDataSource {
    suspend fun getMember(accessToken: String): Result<MemberDto, DataError.Remote>
    suspend fun getClubs(accessToken: String): Result<List<ClubDto>, DataError.Remote>
}
