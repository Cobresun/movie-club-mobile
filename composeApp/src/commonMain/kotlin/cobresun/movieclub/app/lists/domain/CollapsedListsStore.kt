package cobresun.movieclub.app.lists.domain

/**
 * Which lists the user has collapsed on a club's Lists tab (web
 * `useCollapsedLists`). Keyed per club so collapsing Backlog in one club
 * doesn't collapse it in another.
 */
interface CollapsedListsStore {
    suspend fun get(clubSlug: String): Set<String>
    suspend fun set(clubSlug: String, listIds: Set<String>)
}
