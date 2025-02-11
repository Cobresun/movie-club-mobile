package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface MemberRepository {
    suspend fun getMember(): Result<Member, DataError.Remote>
    suspend fun getClubs(): Result<List<Club>, DataError.Remote>
}
