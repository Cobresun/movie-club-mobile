package cobresun.movieclub.app.lists.data.repository

import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.mappers.toWork
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.core.domain.onSuccess
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

class ListsRepositoryImpl(
    private val dataSource: ListsDataSource,
    private val caches: SessionCaches,
) : ListsRepository {
    /** After a write to the club's lists: titles, items, Up Next and reviews may all move. */
    private suspend fun listsChanged(clubSlug: String) = caches.invalidateClubWorks(clubSlug)

    override suspend fun getLists(clubSlug: String): Result<List<WorkList>, DataError.Remote> =
        dataSource.getLists(clubSlug).map { lists ->
            lists.filter { it.systemType == null }.map { it.toWorkList() }
        }

    override suspend fun getItems(clubSlug: String, listId: String): Result<List<Work>, DataError.Remote> =
        dataSource.getItems(clubSlug, listId).let { result ->
            // Each item walks a JSON tree; keep that off the main thread.
            withContext(Dispatchers.Default) {
                val zone = TimeZone.currentSystemDefault()
                result.map { items -> items.map { it.toWork(zone) } }
            }
        }

    override suspend fun getAllItems(clubSlug: String, refresh: Boolean): Result<List<ListedWork>, DataError.Remote> =
        caches.allItems.get(clubSlug, refresh) {
            dataSource.getAllItems(clubSlug).let { result ->
                withContext(Dispatchers.Default) {
                    val zone = TimeZone.currentSystemDefault()
                    result.map { items -> items.map { it.toListedWork(zone) } }
                }
            }
        }

    override suspend fun createList(clubSlug: String, title: String): Result<WorkList, DataError.Remote> =
        dataSource.createList(clubSlug, ListTitleDto(title.trim())).map { it.toWorkList() }
            .onSuccess { listsChanged(clubSlug) }

    override suspend fun renameList(clubSlug: String, listId: String, title: String): Result<Unit, DataError.Remote> =
        dataSource.renameList(clubSlug, listId, ListTitleDto(title.trim())).onSuccess { listsChanged(clubSlug) }

    override suspend fun deleteList(clubSlug: String, listId: String): Result<Unit, DataError.Remote> =
        dataSource.deleteList(clubSlug, listId).onSuccess { listsChanged(clubSlug) }

    override suspend fun reorderLists(clubSlug: String, listIds: List<String>): Result<Unit, DataError.Remote> =
        dataSource.reorderLists(clubSlug, ReorderListsDto(listIds)).onSuccess { listsChanged(clubSlug) }

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
        ).onSuccess { listsChanged(clubSlug) }

    override suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote> =
        dataSource.removeItem(clubSlug, listId, workId).onSuccess { listsChanged(clubSlug) }

    override suspend fun reorderItems(
        clubSlug: String,
        listId: String,
        workIds: List<String>,
    ): Result<Unit, DataError.Remote> =
        dataSource.reorderItems(clubSlug, listId, ReorderItemsDto(workIds)).onSuccess { listsChanged(clubSlug) }

    override suspend fun moveItem(
        clubSlug: String,
        listId: String,
        workId: String,
        destinationListId: String,
    ): Result<Unit, DataError.Remote> =
        dataSource.moveItem(clubSlug, listId, workId, MoveItemDto(destinationListId))
            .onSuccess { listsChanged(clubSlug) }

    override suspend fun getReviewsListId(clubSlug: String): Result<String, DataError.Remote> =
        caches.reviewsListId.get(clubSlug) { dataSource.getReviewsListId(clubSlug).map { it.id } }

    override suspend fun getUpNext(clubSlug: String, refresh: Boolean): Result<String?, DataError.Remote> =
        caches.upNext.get(clubSlug, refresh) { dataSource.getNextWork(clubSlug).map { it.workId } }

    override suspend fun setUpNext(clubSlug: String, workId: String?): Result<Unit, DataError.Remote> {
        val result = if (workId == null) {
            dataSource.clearNextWork(clubSlug)
        } else {
            dataSource.setNextWork(clubSlug, NextWorkDto(workId))
        }
        return result.onSuccess { caches.upNext.put(clubSlug, workId) }
    }
}
