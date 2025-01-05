package cobresun.movieclub.app.clubs.data.network

import cobresun.movieclub.app.clubs.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ClubDataSource {
    suspend fun getMembers(clubId: String): Result<List<MemberDto>, DataError.Remote>
}
