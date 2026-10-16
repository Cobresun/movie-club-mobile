package cobresun.movieclub.app.member.data.repository

import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.member.data.dto.ChangePasswordRequestDto
import cobresun.movieclub.app.member.data.dto.CreateClubRequestDto
import cobresun.movieclub.app.member.data.mappers.toClub
import cobresun.movieclub.app.member.data.mappers.toMember
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository

class MemberRepositoryImpl(
    private val memberDataSource: MemberDataSource
) : MemberRepository, AccountRepository {
    override suspend fun getMember(): Result<Member, DataError.Remote> {
        return memberDataSource.getMember().map { it.toMember() }
    }

    override suspend fun getClubs(): Result<List<Club>, DataError.Remote> {
        return memberDataSource.getClubs()
            .map { clubDtos -> clubDtos.map { it.toClub() } }
    }

    override suspend fun createClub(
        name: String,
        memberEmail: String,
        type: ClubType,
    ): Result<String, DataError.Remote> {
        val request = CreateClubRequestDto(
            name = name.trim(),
            members = listOf(memberEmail),
            type = type.value,
        )
        return memberDataSource.createClub(request)
            .map { response -> response.slug }
    }

    override suspend fun updateName(name: String): Result<Unit, ApiError> =
        memberDataSource.updateName(name.trim())

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit, ApiError> =
        memberDataSource.uploadAvatar(bytes, mimeType)

    override suspend fun removeAvatar(): Result<Unit, ApiError> = memberDataSource.removeAvatar()

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        revokeOtherSessions: Boolean,
    ): Result<Unit, ApiError> = memberDataSource.changePassword(
        ChangePasswordRequestDto(currentPassword, newPassword, revokeOtherSessions)
    )

    override suspend fun requestPasswordReset(email: String): Result<Unit, ApiError> =
        memberDataSource.requestPasswordReset(email.trim())
}
