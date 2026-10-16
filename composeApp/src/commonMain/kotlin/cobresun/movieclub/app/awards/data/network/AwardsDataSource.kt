package cobresun.movieclub.app.awards.data.network

import cobresun.movieclub.app.awards.data.dto.AddCategoryBodyDto
import cobresun.movieclub.app.awards.data.dto.ClubAwardsDto
import cobresun.movieclub.app.awards.data.dto.NominationBodyDto
import cobresun.movieclub.app.awards.data.dto.RankingBodyDto
import cobresun.movieclub.app.awards.data.dto.ReorderCategoriesBodyDto
import cobresun.movieclub.app.awards.data.dto.StepBodyDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart

/**
 * `/api/club/:clubSlug/awards`. The server resolves the club from the slug
 * (`validClubSlug`), so every call uses the slug, never the club id.
 */
interface AwardsDataSource {
    suspend fun getYears(clubSlug: String): Result<List<Int>, DataError.Remote>
    suspend fun getAwards(clubSlug: String, year: Int): Result<ClubAwardsDto, DataError.Remote>
    suspend fun putStep(clubSlug: String, year: Int, body: StepBodyDto): Result<Unit, DataError.Remote>
    suspend fun addCategory(clubSlug: String, year: Int, body: AddCategoryBodyDto): Result<Unit, DataError.Remote>
    suspend fun reorderCategories(clubSlug: String, year: Int, body: ReorderCategoriesBodyDto): Result<Unit, DataError.Remote>
    suspend fun deleteCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote>
    suspend fun addNomination(clubSlug: String, year: Int, body: NominationBodyDto): Result<Unit, DataError.Remote>
    suspend fun deleteNomination(clubSlug: String, year: Int, title: String, movieId: Int, userId: String): Result<Unit, DataError.Remote>
    suspend fun submitRanking(clubSlug: String, year: Int, body: RankingBodyDto): Result<Unit, DataError.Remote>
}

class KtorAwardsDataSource(
    private val httpClient: HttpClient,
) : AwardsDataSource {
    private fun awards(slug: String) = "$BASE_URL/api/club/$slug/awards"
    private fun year(slug: String, year: Int) = "${awards(slug)}/$year"

    override suspend fun getYears(clubSlug: String): Result<List<Int>, DataError.Remote> =
        safeCall { httpClient.get("${awards(clubSlug)}/years") }

    override suspend fun getAwards(clubSlug: String, year: Int): Result<ClubAwardsDto, DataError.Remote> =
        safeCall { httpClient.get(year(clubSlug, year)) }

    override suspend fun putStep(clubSlug: String, year: Int, body: StepBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${year(clubSlug, year)}/step") { setBody(body) } }

    override suspend fun addCategory(clubSlug: String, year: Int, body: AddCategoryBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${year(clubSlug, year)}/category") { setBody(body) } }

    override suspend fun reorderCategories(
        clubSlug: String,
        year: Int,
        body: ReorderCategoriesBodyDto,
    ): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${year(clubSlug, year)}/category") { setBody(body) } }

    override suspend fun deleteCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${year(clubSlug, year)}/category/${title.encodeURLPathPart()}") }

    override suspend fun addNomination(clubSlug: String, year: Int, body: NominationBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${year(clubSlug, year)}/nomination") { setBody(body) } }

    override suspend fun deleteNomination(
        clubSlug: String,
        year: Int,
        title: String,
        movieId: Int,
        userId: String,
    ): Result<Unit, DataError.Remote> =
        safeCall {
            httpClient.delete("${year(clubSlug, year)}/nomination/$movieId") {
                parameter("awardTitle", title)
                parameter("userId", userId)
            }
        }

    override suspend fun submitRanking(clubSlug: String, year: Int, body: RankingBodyDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${year(clubSlug, year)}/ranking") { setBody(body) } }
}
