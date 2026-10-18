package cobresun.movieclub.app.lists.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.NewWork

/** A user-managed list of works in a club (e.g. "Watch List"). The reviews system list is excluded. */
data class WorkList(
    val id: String,
    val title: String,
    val itemCount: Int,
)

/** A work on one of the club's user lists, tagged with that list. */
data class ListedWork(
    val work: Work,
    val listId: String,
    val listTitle: String,
)

/**
 * Club lists, their items and the club-wide "Up Next" pointer.
 * All operations address the club by slug.
 */
interface ListsRepository {
    /** All user lists in the club's display order. */
    suspend fun getLists(clubSlug: String): Result<List<WorkList>, DataError.Remote>

    /** Items on one list in the list's saved order. */
    suspend fun getItems(clubSlug: String, listId: String): Result<List<Work>, DataError.Remote>

    /** Creates, renames, deletes or reorders lists. */
    suspend fun createList(clubSlug: String, title: String): Result<WorkList, DataError.Remote>
    suspend fun renameList(clubSlug: String, listId: String, title: String): Result<Unit, DataError.Remote>
    suspend fun deleteList(clubSlug: String, listId: String): Result<Unit, DataError.Remote>
    suspend fun reorderLists(clubSlug: String, listIds: List<String>): Result<Unit, DataError.Remote>

    /**
     * Items across every user list (reviews excluded), in list display order then item order.
     * Like [getUpNext], tabs asking within a minute share one answer unless [refresh];
     * writes here renew it.
     */
    suspend fun getAllItems(clubSlug: String, refresh: Boolean = false): Result<List<ListedWork>, DataError.Remote>

    /** Adds a work to a list. Fails if it's already on that list. */
    suspend fun addItem(clubSlug: String, listId: String, work: NewWork): Result<Unit, DataError.Remote>
    suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote>
    suspend fun reorderItems(clubSlug: String, listId: String, workIds: List<String>): Result<Unit, DataError.Remote>

    /**
     * Moves a work between lists. [destinationListId] may be [getReviewsListId]'s
     * id, which is how a watched work becomes a review.
     */
    suspend fun moveItem(
        clubSlug: String,
        listId: String,
        workId: String,
        destinationListId: String,
    ): Result<Unit, DataError.Remote>

    /** Id of the hidden reviews system list. */
    suspend fun getReviewsListId(clubSlug: String): Result<String, DataError.Remote>

    /** The club-wide "Up Next" work id, or null when unset. Pass null to [setUpNext] to clear it. */
    suspend fun getUpNext(clubSlug: String, refresh: Boolean = false): Result<String?, DataError.Remote>
    suspend fun setUpNext(clubSlug: String, workId: String?): Result<Unit, DataError.Remote>
}
