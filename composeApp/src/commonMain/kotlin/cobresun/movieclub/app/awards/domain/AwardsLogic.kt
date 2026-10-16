package cobresun.movieclub.app.awards.domain

/** How many movies each member may nominate per category (web: `NOMINATIONS_PER_AWARD`). */
const val NOMINATIONS_PER_MEMBER = 2

/** A nomination's place in a category's final results. */
data class Standing(
    val nomination: Nomination,
    /** Sum of every voter's rank; lower is better. */
    val points: Int,
    /** Competition ranking: ties share a place and the next place is skipped (1, 1, 3). */
    val place: Int,
) {
    val isWinner: Boolean get() = place == 1
}

/**
 * Final results for a category, best first. Matches the web: a nomination's
 * score is the sum of all ranks it received, and the lowest total wins. Ties
 * are broken only for display order (by title) and share a place.
 */
fun AwardCategory.standings(): List<Standing> {
    val scored = nominations
        .map { it to it.ranking.values.sum() }
        .sortedWith(compareBy<Pair<Nomination, Int>> { it.second }.thenBy { it.first.title.lowercase() })
    return scored.map { (nomination, points) ->
        Standing(nomination, points, place = scored.indexOfFirst { it.second == points } + 1)
    }
}

/** Every nomination with the best (lowest) total. Empty when there are no nominations. */
fun AwardCategory.winners(): List<Nomination> = standings().filter { it.isWinner }.map { it.nomination }

/**
 * Nominations in [userId]'s ranked order: ranked ones by rank, then unranked
 * ones in their nomination order. Used to seed the ranking editor.
 */
fun AwardCategory.rankingOrderFor(userId: String): List<Nomination> {
    val (ranked, unranked) = nominations.partition { it.ranking[userId] != null }
    return ranked.sortedBy { it.ranking.getValue(userId) } + unranked
}

/** Whether [userId] has ranked every nomination in this category. */
fun AwardCategory.isRankedBy(userId: String): Boolean =
    nominations.all { it.ranking[userId] != null }

/** The members (by id) who have ranked every nomination here. */
fun AwardCategory.votersDone(memberIds: Collection<String>): Int = memberIds.count { isRankedBy(it) }

/** [userId]'s own nominations in this category. */
fun AwardCategory.nominationsBy(userId: String): List<Nomination> =
    nominations.filter { userId in it.nominatedBy }

/** How many more movies [userId] may nominate here. */
fun AwardCategory.remainingNominationsFor(userId: String): Int =
    (NOMINATIONS_PER_MEMBER - nominationsBy(userId).size).coerceAtLeast(0)

/** Whether every member has fully ranked every category (web's gate for leaving Rankings). */
fun AwardsSeason.isRankingComplete(memberIds: Collection<String>): Boolean =
    categories.all { category -> memberIds.all { category.isRankedBy(it) } }

/** Whether the season may move on from its current step (mirrors the web's button gating). */
fun AwardsSeason.canAdvance(memberIds: Collection<String>): Boolean = when (step) {
    AwardsStep.CATEGORIES -> categories.isNotEmpty()
    AwardsStep.RANKINGS -> isRankingComplete(memberIds)
    AwardsStep.NOMINATIONS -> true
    AwardsStep.PRESENTATION, AwardsStep.COMPLETED -> false
}

/** Whether every category title is in [revealed], i.e. the presentation is over. */
fun AwardsSeason.allRevealed(revealed: Set<String>): Boolean =
    categories.all { it.title in revealed }

/** Category titles are the server's keys: they must be non-blank and unique (case-insensitive). */
fun AwardsSeason.validateCategoryTitle(title: String, except: String? = null): CategoryTitleError? {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return CategoryTitleError.BLANK
    val taken = categories.any { it.title != except && it.title.equals(trimmed, ignoreCase = true) }
    return if (taken) CategoryTitleError.DUPLICATE else null
}

enum class CategoryTitleError { BLANK, DUPLICATE }
