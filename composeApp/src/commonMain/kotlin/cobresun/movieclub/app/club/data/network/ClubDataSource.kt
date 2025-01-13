package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ClubDataSource {
    suspend fun getMembers(clubId: String): Result<List<MemberDto>, DataError.Remote>
}
