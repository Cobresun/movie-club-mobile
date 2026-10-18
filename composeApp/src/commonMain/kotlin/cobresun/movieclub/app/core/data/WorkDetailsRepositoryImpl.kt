package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.data.mappers.toWorkDetails
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkDetailsRepository
import cobresun.movieclub.app.core.domain.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonElement

/** `GET /api/club/:slug/work/:workId/details`, shared by the reviews and lists sheets. */
class WorkDetailsRepositoryImpl(
    private val httpClient: HttpClient,
    private val offline: OfflineStore,
) : WorkDetailsRepository {
    override suspend fun getWorkDetails(clubSlug: String, workId: String): Result<WorkDetails?, DataError.Remote> =
        offline.readThrough(OfflineKeys.workDetails(clubSlug, workId), JsonElement.serializer()) {
            safeCall<JsonElement> { httpClient.get("$BASE_URL/api/club/$clubSlug/work/$workId/details") }
        }.map { it.toWorkDetails() }
}
