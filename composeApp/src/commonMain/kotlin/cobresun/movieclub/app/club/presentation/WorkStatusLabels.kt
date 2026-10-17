package cobresun.movieclub.app.club.presentation

/**
 * Where a search result already lives in the club, keyed by external id, e.g.
 * "Reviewed" or "In Spooky Season". Works can be on several lists, so this is
 * informational: the destination itself is excluded (it's shown as "added").
 *
 * @param reviewedIds external ids on the reviews list (omit when adding a review)
 * @param listed (externalId, listId, listTitle) for every list item in the club
 * @param destinationListId the list being added to, if any
 */
fun workStatusLabels(
    reviewedIds: Set<String>,
    listed: List<Triple<String, String, String>>,
    destinationListId: String?,
): Map<String, String> {
    val lists = listed
        .filter { (_, listId, _) -> listId != destinationListId }
        .groupBy({ it.first }, { it.third })
    return (reviewedIds + lists.keys).associateWith { id ->
        when {
            id in reviewedIds -> "Reviewed"
            else -> "In " + lists.getValue(id).distinct().joinToString(", ")
        }
    }
}
