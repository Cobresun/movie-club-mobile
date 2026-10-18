package cobresun.movieclub.app.member.data.repository

import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.data.OfflineKeys
import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.readThrough
import cobresun.movieclub.app.core.data.saved
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.member.data.dto.ClubDto
import kotlinx.serialization.builtins.ListSerializer
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.data.dto.ChangePasswordRequestDto
import cobresun.movieclub.app.member.data.dto.CreateClubRequestDto
import cobresun.movieclub.app.member.data.mappers.toClub
import cobresun.movieclub.app.member.data.mappers.toMember
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository

class MemberRepositoryImpl(
    private val memberDataSource: MemberDataSource,
    private val caches: SessionCaches,
) : MemberRepository, AccountRepository {
    private val clubsSerializer = ListSerializer(ClubDto.serializer())

    override suspend fun getMember(refresh: Boolean): Result<Member, DataError.Remote> =
        caches.me.get(Unit, refresh) {
            caches.offline.readThrough(OfflineKeys.ME, MemberDto.serializer()) { memberDataSource.getMember() }
                .map { it.toMember() }
        }

    override suspend fun getClubs(refresh: Boolean): Result<List<Club>, DataError.Remote> =
        caches.clubs.get(Unit, refresh) {
            caches.offline.readThrough(OfflineKeys.CLUBS, clubsSerializer) { memberDataSource.getClubs() }
                .map { clubDtos -> clubDtos.map { it.toClub() } }
        }

    override suspend fun savedMember(): Member? = caches.me.saved(Unit) {
        caches.offline.load(OfflineKeys.ME, MemberDto.serializer())?.toMember()
    }

    override suspend fun savedClubs(): List<Club>? = caches.clubs.saved(Unit) {
        caches.offline.load(OfflineKeys.CLUBS, clubsSerializer)?.map { it.toClub() }
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
            .onSuccess { caches.clubs.invalidate(Unit) }
    }

    /** Name and avatar show in the session user and in every club's member list. */
    private suspend fun profileChanged() {
        caches.me.invalidate(Unit)
        caches.members.invalidateAll()
    }

    override suspend fun updateName(name: String): Result<Unit, ApiError> =
        memberDataSource.updateName(name.trim()).onSuccess { profileChanged() }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit, ApiError> =
        memberDataSource.uploadAvatar(bytes, mimeType).onSuccess { profileChanged() }

    override suspend fun removeAvatar(): Result<Unit, ApiError> =
        memberDataSource.removeAvatar().onSuccess { profileChanged() }

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
