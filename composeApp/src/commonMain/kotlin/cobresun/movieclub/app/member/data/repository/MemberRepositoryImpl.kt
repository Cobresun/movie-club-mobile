package cobresun.movieclub.app.member.data.repository

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.member.data.mappers.toClub
import cobresun.movieclub.app.member.data.mappers.toMember
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository

class MemberRepositoryImpl(
    private val memberDataSource: MemberDataSource
) : MemberRepository {
    override suspend fun getMember(
    ): Result<Member, DataError.Remote> {
        return memberDataSource.getMember().map { it.toMember() }
    }

    override suspend fun getClubs(
    ): Result<List<Club>, DataError.Remote> {
        return memberDataSource.getClubs()
            .map { clubDtos -> clubDtos.map { it.toClub() } }
    }
}
