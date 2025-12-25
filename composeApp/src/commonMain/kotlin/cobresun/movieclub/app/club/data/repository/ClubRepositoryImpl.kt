package cobresun.movieclub.app.club.data.repository

import cobresun.movieclub.app.club.data.mappers.toDomain
import cobresun.movieclub.app.club.data.network.ClubDataSource
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.member.domain.Member

class ClubRepositoryImpl(
    private val clubDataSource: ClubDataSource
) : ClubRepository {
    override suspend fun getMembers(clubId: String): Result<List<Member>, DataError.Remote> {
        return clubDataSource.getMembers(clubId)
            .map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun generateInviteToken(clubId: String): Result<String, DataError.Remote> {
        return clubDataSource.generateInviteToken(clubId)
            .map { it.token }
    }

    override suspend fun leaveClub(clubId: String): Result<Unit, DataError.Remote> {
        return clubDataSource.leaveClub(clubId)
    }
}
