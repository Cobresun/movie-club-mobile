package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.ClubPreviewDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsPatchDto
import cobresun.movieclub.app.club.data.dto.InviteInfoDto
import cobresun.movieclub.app.club.data.dto.InviteTokenDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.data.dto.SlugResponseDto
import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface ClubDataSource {
    /** Kept on DataError.Remote: also used by ReviewsRepositoryImpl. */
    suspend fun getMembers(clubSlug: String): Result<List<MemberDto>, DataError.Remote>
    suspend fun getClubPreview(clubSlug: String): Result<ClubPreviewDto, DataError.Remote>
    suspend fun generateInviteToken(clubSlug: String): Result<InviteTokenDto, ApiError>
    suspend fun leaveClub(clubSlug: String): Result<Unit, ApiError>
    suspend fun removeMember(clubSlug: String, memberId: String): Result<Unit, ApiError>
    suspend fun updateName(clubSlug: String, name: String): Result<Unit, ApiError>
    suspend fun updateSlug(clubSlug: String, slug: String): Result<SlugResponseDto, ApiError>
    suspend fun getSettings(clubSlug: String): Result<ClubSettingsDto, ApiError>
    suspend fun updateSettings(clubSlug: String, patch: ClubSettingsPatchDto): Result<ClubSettingsDto, ApiError>
    suspend fun getInviteInfo(token: String): Result<InviteInfoDto, ApiError>
    suspend fun joinClub(token: String): Result<Unit, ApiError>
}
