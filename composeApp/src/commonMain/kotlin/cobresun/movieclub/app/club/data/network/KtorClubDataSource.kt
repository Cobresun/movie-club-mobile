package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.ClubPreviewDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsPatchDto
import cobresun.movieclub.app.club.data.dto.InviteInfoDto
import cobresun.movieclub.app.club.data.dto.InviteTokenDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.data.dto.NameRequestDto
import cobresun.movieclub.app.club.data.dto.SlugRequestDto
import cobresun.movieclub.app.club.data.dto.SlugResponseDto
import cobresun.movieclub.app.club.data.dto.TokenRequestDto
import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart

class KtorClubDataSource(
    private val httpClient: HttpClient
) : ClubDataSource {
    private fun club(slug: String) = "$BASE_URL/api/club/${slug.encodeURLPathPart()}"

    override suspend fun getMembers(clubSlug: String): Result<List<MemberDto>, DataError.Remote> =
        apiCall<List<MemberDto>> { httpClient.get("${club(clubSlug)}/members") }.toRemote()

    override suspend fun getClubPreview(clubSlug: String): Result<ClubPreviewDto, DataError.Remote> =
        apiCall<ClubPreviewDto> { httpClient.get(club(clubSlug)) }.toRemote()

    override suspend fun generateInviteToken(clubSlug: String): Result<InviteTokenDto, ApiError> =
        apiCall { httpClient.post("${club(clubSlug)}/invite") }

    override suspend fun leaveClub(clubSlug: String): Result<Unit, ApiError> =
        apiCall { httpClient.delete("${club(clubSlug)}/members/self") }

    override suspend fun removeMember(clubSlug: String, memberId: String): Result<Unit, ApiError> =
        apiCall { httpClient.delete("${club(clubSlug)}/members/${memberId.encodeURLPathPart()}") }

    override suspend fun updateName(clubSlug: String, name: String): Result<Unit, ApiError> =
        apiCall { httpClient.put("${club(clubSlug)}/name") { setBody(NameRequestDto(name)) } }

    override suspend fun updateSlug(clubSlug: String, slug: String): Result<SlugResponseDto, ApiError> =
        apiCall { httpClient.put("${club(clubSlug)}/slug") { setBody(SlugRequestDto(slug)) } }

    override suspend fun getSettings(clubSlug: String): Result<ClubSettingsDto, ApiError> =
        apiCall { httpClient.get("${club(clubSlug)}/settings") }

    override suspend fun updateSettings(
        clubSlug: String,
        patch: ClubSettingsPatchDto
    ): Result<ClubSettingsDto, ApiError> =
        apiCall { httpClient.post("${club(clubSlug)}/settings") { setBody(patch) } }

    override suspend fun getInviteInfo(token: String): Result<InviteInfoDto, ApiError> =
        apiCall { httpClient.get("$BASE_URL/api/club/joinInfo/${token.encodeURLPathPart()}") }

    override suspend fun joinClub(token: String): Result<Unit, ApiError> =
        apiCall { httpClient.post("$BASE_URL/api/club/join") { setBody(TokenRequestDto(token)) } }
}
