package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ChangePasswordRequestDto
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.data.dto.CreateClubRequestDto
import cobresun.movieclub.app.member.data.dto.CreateClubResponseDto

interface MemberDataSource {
    suspend fun getMember(): Result<MemberDto, DataError.Remote>
    suspend fun getClubs(): Result<List<ClubDto>, DataError.Remote>
    suspend fun createClub(request: CreateClubRequestDto): Result<CreateClubResponseDto, DataError.Remote>
    suspend fun updateName(name: String): Result<Unit, ApiError>
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit, ApiError>
    suspend fun removeAvatar(): Result<Unit, ApiError>
    suspend fun changePassword(request: ChangePasswordRequestDto): Result<Unit, ApiError>
    suspend fun requestPasswordReset(email: String): Result<Unit, ApiError>
}
