package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.InviteTokenDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post

class KtorClubDataSource(
    private val httpClient: HttpClient
) : ClubDataSource {
    override suspend fun getMembers(clubSlug: String): Result<List<MemberDto>, DataError.Remote> {
        return safeCall<List<MemberDto>> {
            httpClient.get(
                urlString = "$BASE_URL/api/club/$clubSlug/members"
            )
        }
    }

    override suspend fun generateInviteToken(clubSlug: String): Result<InviteTokenDto, DataError.Remote> {
        return safeCall<InviteTokenDto> {
            httpClient.post(
                urlString = "$BASE_URL/api/club/$clubSlug/invite"
            )
        }
    }

    override suspend fun leaveClub(clubSlug: String): Result<Unit, DataError.Remote> {
        return safeCall<Unit> {
            httpClient.delete(
                urlString = "$BASE_URL/api/club/$clubSlug/members/self"
            )
        }
    }
}
