package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.data.dto.CreateClubRequestDto
import cobresun.movieclub.app.member.data.dto.CreateClubResponseDto

interface MemberDataSource {
    suspend fun getMember(): Result<MemberDto, DataError.Remote>
    suspend fun getClubs(): Result<List<ClubDto>, DataError.Remote>
    suspend fun createClub(request: CreateClubRequestDto): Result<CreateClubResponseDto, DataError.Remote>
}
