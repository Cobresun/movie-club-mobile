package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.InviteTokenDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ClubDataSource {
    suspend fun getMembers(clubId: String): Result<List<MemberDto>, DataError.Remote>
    suspend fun generateInviteToken(clubId: String): Result<InviteTokenDto, DataError.Remote>
    suspend fun leaveClub(clubId: String): Result<Unit, DataError.Remote>
}
