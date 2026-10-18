package cobresun.movieclub.app.lists.presentation

import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.filters.domain.ClubFilters
import cobresun.movieclub.app.filters.domain.FilterQuery
import cobresun.movieclub.app.filters.domain.FilterResults
import cobresun.movieclub.app.filters.domain.FilterTarget
import cobresun.movieclub.app.filters.domain.computeFilters
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.DerivedMemo
import cobresun.movieclub.app.reviews.presentation.CommentsState

/* What the Lists tab shows (ListsState), what it can do (ListsAction) and its one-off events. */

data class ListsState(
    val clubType: ClubType = ClubType.MOVIE,
    val clubName: String = "",
    val lists: AsyncResult<List<WorkList>> = AsyncResult.Loading,
    val itemsByList: Map<String, AsyncResult<List<Work>>> = emptyMap(),
    /** Lists the user folded away (persisted per club, like web). */
    val collapsedListIds: Set<String> = emptySet(),
    val upNextWorkId: String? = null,
    /** Search text and filter picks; they apply to every list at once, like web's bar above the lists. */
    val filterQuery: FilterQuery = FilterQuery(),
    val isRefreshing: Boolean = false,
    /** Club members by id, for "Added by". */
    val members: Map<String, Member> = emptyMap(),
    /** The card to glow once: just pinned as Up Next, or just dropped. */
    val pulse: CardPulse? = null,
    /** Non-null while the random picker runs. */
    val shuffle: ShuffleState? = null,
    /** The signed-in member, for their own comments. */
    val currentUserId: String? = null,
    /** The open item details sheet. */
    val detail: ListItemDetailState? = null,
    /** Every item across the user lists, for picking a review in the Add review sheet. */
    val allItems: AsyncResult<List<ListedWork>> = AsyncResult.Loading,
    /** Derived values, kept across copies; see [DerivedMemo]. */
    private val memo: DerivedMemo = DerivedMemo(),
) {
    /** [allItems] with the Up Next work first, then list order. */
    val pickableItems: List<ListedWork>
        get() = memo.get("pickable", allItems, upNextWorkId) {
            (allItems as? AsyncResult.Success)?.data.orEmpty().sortedByDescending { it.work.id == upNextWorkId }
        }

    val listsOrEmpty: List<WorkList>
        get() = (lists as? AsyncResult.Success)?.data.orEmpty()

    fun list(listId: String?): WorkList? = listsOrEmpty.firstOrNull { it.id == listId }

    fun items(listId: String): AsyncResult<List<Work>> = itemsByList[listId] ?: AsyncResult.Loading

    fun itemsOrEmpty(listId: String): List<Work> = (itemsByList[listId] as? AsyncResult.Success)?.data.orEmpty()

    /**
     * The filter bar over every item on every list (a work on two lists counts
     * once). Scores and review dates don't apply to watchlist items, so those
     * filters are left out, as on web.
     */
    val filters: FilterResults<Work>
        get() = memo.get("filters", lists, itemsByList, filterQuery, clubType) {
        computeFilters(
            items = listsOrEmpty.flatMap { itemsOrEmpty(it.id) },
            query = filterQuery,
            clubType = clubType,
            excludeKeys = ClubFilters.reviewOnlyKeys,
            id = { it.id },
            target = { FilterTarget(it) },
        )
        }

    /** Ids of the works matching the filters, or null when nothing is filtering (show everything). */
    private val matchingIds: Set<String>?
        get() = filters.let { f -> memo.get("matching", f) { if (f.isActive) f.filtered.mapTo(HashSet()) { it.id } else null } }

    /** A list's items that match the filters, in list order. */
    fun visibleItems(listId: String): List<Work> {
        val ids = matchingIds ?: return itemsOrEmpty(listId)
        val items = itemsOrEmpty(listId)
        return memo.get("visible-$listId", items, ids) { items.filter { it.id in ids } }
    }

    fun isCollapsed(listId: String): Boolean = listId in collapsedListIds
}

data class ListItemDetailState(
    val listId: String,
    val workId: String,
    /** Full metadata incl. cast; the sheet shows the list summary while this loads. */
    val details: AsyncResult<WorkDetails?> = AsyncResult.Loading,
    val thread: CommentsState = CommentsState(),
)

/** [id] is fresh per pulse so the same card can glow twice in a row. */
data class CardPulse(val workId: String, val id: Int)

data class ShuffleState(val listId: String, val candidates: List<Work>, val selected: Work)

sealed interface ListsEvent {
    data class Message(val message: UiMessage) : ListsEvent

    /** A work left the lists for the reviews tab. */
    data class MovedToReviews(val title: String) : ListsEvent

    /** Open the system share sheet for a public link. */
    data class Share(val url: String, val title: String) : ListsEvent
}

sealed interface ListsAction {
    data object Refresh : ListsAction
    data class Filter(val query: FilterQuery) : ListsAction
    data class ToggleCollapsed(val listId: String) : ListsAction

    data class CreateList(val title: String) : ListsAction
    data class RenameList(val listId: String, val title: String) : ListsAction
    data class DeleteList(val listId: String) : ListsAction
    data class MoveList(val listId: String, val offset: Int) : ListsAction

    data class AddWork(val work: NewWork, val listId: String) : ListsAction
    data class RemoveWork(val work: Work, val listId: String) : ListsAction
    data class SetUpNext(val work: Work) : ListsAction
    data object ClearUpNext : ListsAction
    data class MoveWork(val work: Work, val fromListId: String, val destinationListId: String) : ListsAction
    /** Moves [work] from [fromListId] into reviews. */
    data class MoveToReviews(val work: Work, val fromListId: String) : ListsAction

    /** (Re)loads [ListsState.allItems] for the Add review sheet. */
    data object LoadAllItems : ListsAction

    /**
     * A drag-and-drop finished: [workIds] are the list's shown items (all of
     * them, or those matching the filter) in their new order; [droppedId] is
     * the card that was dragged.
     */
    data class Reorder(val listId: String, val workIds: List<String>, val droppedId: String) : ListsAction

    data class Shuffle(val listId: String) : ListsAction
    data class ShareList(val listId: String) : ListsAction
    /** The random picker closed (its choices dispatch their own actions). */
    data object CloseShuffle : ListsAction

    data class OpenDetail(val listId: String, val workId: String) : ListsAction
    data object CloseDetail : ListsAction
    data class AddComment(val content: String, val spoiler: Boolean) : ListsAction
    data class EditComment(val commentId: String, val content: String, val spoiler: Boolean) : ListsAction
    data class DeleteComment(val commentId: String) : ListsAction
    data object RetryComments : ListsAction
    /** The card glow ran; clear it so a card scrolled back into view doesn't glow again. */
    data object PulseShown : ListsAction
}

/** Id prefix of a work still being added (web `OPTIMISTIC_WORK_ID`); acting on it would send a fake id. */
internal const val PENDING_PREFIX = "pending-"

/** Whether this card is a work still being added. */
val Work.isPending: Boolean get() = id.startsWith(PENDING_PREFIX)

/** A list-item change captured with all its ids, so it can be replayed verbatim on Retry. */
sealed interface ListItemOp {
    val work: Work
    val sourceListId: String

    data class Remove(override val work: Work, override val sourceListId: String) : ListItemOp
    data class Move(override val work: Work, override val sourceListId: String, val destinationListId: String) : ListItemOp
}
