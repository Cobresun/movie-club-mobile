package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ClubDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class KtorMemberDataSource(
    private val httpClient: HttpClient
) : MemberDataSource {
    override suspend fun getMember(): Result<MemberDto, DataError.Remote> {
        return safeCall<MemberDto> {
            httpClient.get("$BASE_URL/api/member")
        }
    }

    override suspend fun getClubs(): Result<List<ClubDto>, DataError.Remote> {
        return safeCall<List<ClubDto>> {
            httpClient.get("$BASE_URL/api/member/clubs")
        }
    }
}
