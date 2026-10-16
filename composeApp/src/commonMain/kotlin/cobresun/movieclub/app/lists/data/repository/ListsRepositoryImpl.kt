package cobresun.movieclub.app.lists.data.repository

import cobresun.movieclub.app.core.data.mappers.toWork
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.lists.data.dto.ListInsertDto
import cobresun.movieclub.app.lists.data.dto.ListTitleDto
import cobresun.movieclub.app.lists.data.dto.MoveItemDto
import cobresun.movieclub.app.lists.data.dto.NextWorkDto
import cobresun.movieclub.app.lists.data.dto.ReorderItemsDto
import cobresun.movieclub.app.lists.data.dto.ReorderListsDto
import cobresun.movieclub.app.lists.data.mappers.toWorkList
import cobresun.movieclub.app.lists.data.network.ListsDataSource
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.lists.data.mappers.toListedWork

class ListsRepositoryImpl(
    private val dataSource: ListsDataSource,
) : ListsRepository {
    private val reviewsListIds = mutableMapOf<String, String>()

    override suspend fun getLists(clubSlug: String): Result<List<WorkList>, DataError.Remote> =
        dataSource.getLists(clubSlug).map { lists ->
            lists.filter { it.systemType == null }.map { it.toWorkList() }
        }

    override suspend fun getItems(clubSlug: String, listId: String): Result<List<Work>, DataError.Remote> =
        dataSource.getItems(clubSlug, listId).map { items -> items.map { it.toWork() } }

    override suspend fun getAllItems(clubSlug: String): Result<List<ListedWork>, DataError.Remote> =
        dataSource.getAllItems(clubSlug).map { items -> items.map { it.toListedWork() } }

    override suspend fun createList(clubSlug: String, title: String): Result<WorkList, DataError.Remote> =
        dataSource.createList(clubSlug, ListTitleDto(title.trim())).map { it.toWorkList() }

    override suspend fun renameList(clubSlug: String, listId: String, title: String): Result<Unit, DataError.Remote> =
        dataSource.renameList(clubSlug, listId, ListTitleDto(title.trim()))

    override suspend fun deleteList(clubSlug: String, listId: String): Result<Unit, DataError.Remote> =
        dataSource.deleteList(clubSlug, listId)

    override suspend fun reorderLists(clubSlug: String, listIds: List<String>): Result<Unit, DataError.Remote> =
        dataSource.reorderLists(clubSlug, ReorderListsDto(listIds))

    override suspend fun addItem(clubSlug: String, listId: String, work: NewWork): Result<Unit, DataError.Remote> =
        dataSource.addItem(
            clubSlug,
            listId,
            ListInsertDto(
                type = work.type.value,
                title = work.title,
                externalId = work.externalId,
                imageUrl = work.imageUrl,
            ),
        )

    override suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote> =
        dataSource.removeItem(clubSlug, listId, workId)

    override suspend fun reorderItems(
        clubSlug: String,
        listId: String,
        workIds: List<String>,
    ): Result<Unit, DataError.Remote> = dataSource.reorderItems(clubSlug, listId, ReorderItemsDto(workIds))

    override suspend fun moveItem(
        clubSlug: String,
        listId: String,
        workId: String,
        destinationListId: String,
    ): Result<Unit, DataError.Remote> =
        dataSource.moveItem(clubSlug, listId, workId, MoveItemDto(destinationListId))

    override suspend fun getReviewsListId(clubSlug: String): Result<String, DataError.Remote> {
        reviewsListIds[clubSlug]?.let { return Result.Success(it) }
        return dataSource.getReviewsListId(clubSlug).map { dto ->
            reviewsListIds[clubSlug] = dto.id
            dto.id
        }
    }

    override suspend fun getUpNext(clubSlug: String): Result<String?, DataError.Remote> =
        dataSource.getNextWork(clubSlug).map { it.workId }

    override suspend fun setUpNext(clubSlug: String, workId: String?): Result<Unit, DataError.Remote> =
        if (workId == null) {
            dataSource.clearNextWork(clubSlug)
        } else {
            dataSource.setNextWork(clubSlug, NextWorkDto(workId))
        }
}
