package cobresun.movieclub.app.lists.domain

/**
 * Applies a reorder of some of a list's items (e.g. only those matching a
 * filter) to the whole list, the way the server does for
 * `PUT list/:id/reorder`: the reordered items take over the slots the subset
 * held, in their new order, and every other item stays where it was.
 */
fun <T, K> List<T>.withSubsetReordered(reordered: List<T>, key: (T) -> K): List<T> {
    val order = reordered.map(key)
    val moving = order.toSet()
    val byKey = reordered.associateBy(key)
    val next = order.iterator()
    return map { item -> if (key(item) in moving && next.hasNext()) byKey.getValue(next.next()) else item }
}

/**
 * The list with [workId] first and the rest in their current order, which is
 * how the web saves a list when its work becomes Up Next.
 */
fun <T> List<T>.withFirst(workId: String, id: (T) -> String): List<T> {
    val target = firstOrNull { id(it) == workId } ?: return this
    return listOf(target) + filter { id(it) != workId }
}
