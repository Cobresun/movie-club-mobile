package cobresun.movieclub.app.lists.data.repository

import cobresun.movieclub.app.core.data.OfflineKeys
import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.readThrough
import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.lists.data.dto.SourcedListItemDto
import cobresun.movieclub.app.lists.data.dto.WorkListDto
import kotlinx.serialization.builtins.ListSerializer
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

    private val listsSerializer = ListSerializer(WorkListDto.serializer())
    private val itemsSerializer = ListSerializer(WorkListItemDto.serializer())
    private val allItemsSerializer = ListSerializer(SourcedListItemDto.serializer())

    override suspend fun getLists(clubSlug: String): Result<List<WorkList>, DataError.Remote> =
        caches.offline.readThrough(OfflineKeys.lists(clubSlug), listsSerializer) { dataSource.getLists(clubSlug) }
            .map { it.toWorkLists() }

    override suspend fun savedLists(clubSlug: String): List<WorkList>? =
        caches.offline.load(OfflineKeys.lists(clubSlug), listsSerializer)?.toWorkLists()

    private fun List<WorkListDto>.toWorkLists() = filter { it.systemType == null }.map { it.toWorkList() }

    override suspend fun getItems(clubSlug: String, listId: String): Result<List<Work>, DataError.Remote> =
        caches.offline.readThrough(OfflineKeys.listItems(clubSlug, listId), itemsSerializer) {
            dataSource.getItems(clubSlug, listId)
        }.map { it.toWorks() }

    override suspend fun savedItems(clubSlug: String, listId: String): List<Work>? =
        caches.offline.load(OfflineKeys.listItems(clubSlug, listId), itemsSerializer)?.toWorks()

    // Each item walks a JSON tree; keep that off the main thread.
    private suspend fun List<WorkListItemDto>.toWorks(): List<Work> = withContext(Dispatchers.Default) {
        val zone = TimeZone.currentSystemDefault()
        map { it.toWork(zone) }
    }

    override suspend fun getAllItems(clubSlug: String, refresh: Boolean): Result<List<ListedWork>, DataError.Remote> =
        caches.allItems.get(clubSlug, refresh) {
            caches.offline.readThrough(OfflineKeys.allItems(clubSlug), allItemsSerializer) { dataSource.getAllItems(clubSlug) }
                .map { items ->
                    withContext(Dispatchers.Default) {
                        val zone = TimeZone.currentSystemDefault()
                        items.map { it.toListedWork(zone) }
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
        caches.upNext.get(clubSlug, refresh) {
            caches.offline.readThrough(OfflineKeys.upNext(clubSlug), NextWorkDto.serializer()) { dataSource.getNextWork(clubSlug) }
                .map { it.workId }
        }

    override suspend fun setUpNext(clubSlug: String, workId: String?): Result<Unit, DataError.Remote> {
        val result = if (workId == null) {
            dataSource.clearNextWork(clubSlug)
        } else {
            dataSource.setNextWork(clubSlug, NextWorkDto(workId))
        }
        return result.onSuccess {
            caches.upNext.put(clubSlug, workId)
            caches.offline.save(OfflineKeys.upNext(clubSlug), NextWorkDto.serializer(), NextWorkDto(workId))
        }
    }
}
