package cobresun.movieclub.app.club.data.repository

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.club.data.dto.ClubFeaturesPatchDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsPatchDto
import cobresun.movieclub.app.club.data.mappers.toDomain
import cobresun.movieclub.app.club.data.network.ClubDataSource
import cobresun.movieclub.app.club.data.network.toRemote
import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.domain.InviteInfo
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.member.domain.Member

class ClubRepositoryImpl(
    private val clubDataSource: ClubDataSource
) : ClubRepository {
    override suspend fun getMembers(clubSlug: String): Result<List<Member>, DataError.Remote> =
        clubDataSource.getMembers(clubSlug).map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getClubPreview(clubSlug: String): Result<Club, DataError.Remote> =
        clubDataSource.getClubPreview(clubSlug).map { Club(id = it.clubId, slug = it.slug, name = it.clubName, type = ClubType.from(it.type)) }

    override suspend fun generateInviteToken(clubSlug: String): Result<String, DataError.Remote> =
        clubDataSource.generateInviteToken(clubSlug).toRemote().map { it.token }

    override suspend fun leaveClub(clubSlug: String): Result<Unit, DataError.Remote> =
        clubDataSource.leaveClub(clubSlug).toRemote()

    override suspend fun removeMember(clubSlug: String, memberId: String): Result<Unit, DataError.Remote> =
        clubDataSource.removeMember(clubSlug, memberId).toRemote()

    override suspend fun updateName(clubSlug: String, name: String): Result<Unit, ApiError> =
        clubDataSource.updateName(clubSlug, name.trim())

    override suspend fun updateSlug(clubSlug: String, newSlug: String): Result<String, ApiError> =
        clubDataSource.updateSlug(clubSlug, newSlug).map { it.slug }

    override suspend fun getFeatures(clubSlug: String): Result<ClubFeatures, DataError.Remote> =
        clubDataSource.getSettings(clubSlug).toRemote().map { it.features.toDomain() }

    override suspend fun updateFeatures(
        clubSlug: String,
        awards: Boolean?,
        discussionQuestions: Boolean?,
    ): Result<ClubFeatures, DataError.Remote> =
        clubDataSource.updateSettings(
            clubSlug,
            ClubSettingsPatchDto(ClubFeaturesPatchDto(awards, discussionQuestions))
        ).toRemote().map { it.features.toDomain() }

    override suspend fun getInviteInfo(token: String): Result<InviteInfo, ApiError> =
        clubDataSource.getInviteInfo(token).map { it.toDomain() }

    override suspend fun joinClub(token: String): Result<Unit, ApiError> =
        clubDataSource.joinClub(token)
}
