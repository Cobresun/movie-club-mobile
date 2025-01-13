package cobresun.movieclub.app.club.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class KtorClubDataSource(
    private val httpClient: HttpClient
) : ClubDataSource {
    override suspend fun getMembers(clubId: String): Result<List<MemberDto>, DataError.Remote> {
        return safeCall<List<MemberDto>> {
            httpClient.get(
                urlString = "$BASE_URL/club/$clubId/members"
            )
        }
    }
}
