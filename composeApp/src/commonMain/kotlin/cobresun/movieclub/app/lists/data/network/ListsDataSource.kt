package cobresun.movieclub.app.lists.data.network

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.lists.data.dto.IdDto
import cobresun.movieclub.app.lists.data.dto.ListInsertDto
import cobresun.movieclub.app.lists.data.dto.ListTitleDto
import cobresun.movieclub.app.lists.data.dto.MoveItemDto
import cobresun.movieclub.app.lists.data.dto.NextWorkDto
import cobresun.movieclub.app.lists.data.dto.ReorderItemsDto
import cobresun.movieclub.app.lists.data.dto.ReorderListsDto
import cobresun.movieclub.app.lists.data.dto.WorkListDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface ListsDataSource {
    suspend fun getLists(clubSlug: String): Result<List<WorkListDto>, DataError.Remote>
    suspend fun createList(clubSlug: String, body: ListTitleDto): Result<WorkListDto, DataError.Remote>
    suspend fun renameList(clubSlug: String, listId: String, body: ListTitleDto): Result<Unit, DataError.Remote>
    suspend fun deleteList(clubSlug: String, listId: String): Result<Unit, DataError.Remote>
    suspend fun reorderLists(clubSlug: String, body: ReorderListsDto): Result<Unit, DataError.Remote>
    suspend fun getItems(clubSlug: String, listId: String): Result<List<WorkListItemDto>, DataError.Remote>
    suspend fun addItem(clubSlug: String, listId: String, body: ListInsertDto): Result<Unit, DataError.Remote>
    suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote>
    suspend fun reorderItems(clubSlug: String, listId: String, body: ReorderItemsDto): Result<Unit, DataError.Remote>
    suspend fun moveItem(clubSlug: String, listId: String, workId: String, body: MoveItemDto): Result<Unit, DataError.Remote>
    suspend fun getReviewsListId(clubSlug: String): Result<IdDto, DataError.Remote>
    suspend fun getNextWork(clubSlug: String): Result<NextWorkDto, DataError.Remote>
    suspend fun setNextWork(clubSlug: String, body: NextWorkDto): Result<Unit, DataError.Remote>
    suspend fun clearNextWork(clubSlug: String): Result<Unit, DataError.Remote>
}

class KtorListsDataSource(
    private val httpClient: HttpClient,
) : ListsDataSource {
    private fun club(slug: String) = "$BASE_URL/api/club/$slug"
    private fun list(slug: String) = "${club(slug)}/list"

    override suspend fun getLists(clubSlug: String): Result<List<WorkListDto>, DataError.Remote> =
        safeCall { httpClient.get(list(clubSlug)) }

    override suspend fun createList(clubSlug: String, body: ListTitleDto): Result<WorkListDto, DataError.Remote> =
        safeCall { httpClient.post(list(clubSlug)) { setBody(body) } }

    override suspend fun renameList(clubSlug: String, listId: String, body: ListTitleDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${list(clubSlug)}/$listId") { setBody(body) } }

    override suspend fun deleteList(clubSlug: String, listId: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${list(clubSlug)}/$listId") }

    override suspend fun reorderLists(clubSlug: String, body: ReorderListsDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${list(clubSlug)}/reorder") { setBody(body) } }

    override suspend fun getItems(clubSlug: String, listId: String): Result<List<WorkListItemDto>, DataError.Remote> =
        safeCall { httpClient.get("${list(clubSlug)}/$listId") }

    override suspend fun addItem(clubSlug: String, listId: String, body: ListInsertDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${list(clubSlug)}/$listId/items") { setBody(body) } }

    override suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${list(clubSlug)}/$listId/items/$workId") }

    override suspend fun reorderItems(clubSlug: String, listId: String, body: ReorderItemsDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${list(clubSlug)}/$listId/reorder") { setBody(body) } }

    override suspend fun moveItem(
        clubSlug: String,
        listId: String,
        workId: String,
        body: MoveItemDto,
    ): Result<Unit, DataError.Remote> =
        safeCall { httpClient.post("${list(clubSlug)}/$listId/items/$workId/move") { setBody(body) } }

    override suspend fun getReviewsListId(clubSlug: String): Result<IdDto, DataError.Remote> =
        safeCall { httpClient.get("${list(clubSlug)}/reviews-id") }

    override suspend fun getNextWork(clubSlug: String): Result<NextWorkDto, DataError.Remote> =
        safeCall { httpClient.get("${club(clubSlug)}/nextWork") }

    override suspend fun setNextWork(clubSlug: String, body: NextWorkDto): Result<Unit, DataError.Remote> =
        safeCall { httpClient.put("${club(clubSlug)}/nextWork") { setBody(body) } }

    override suspend fun clearNextWork(clubSlug: String): Result<Unit, DataError.Remote> =
        safeCall { httpClient.delete("${club(clubSlug)}/nextWork") }
}
