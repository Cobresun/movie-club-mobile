package cobresun.movieclub.app.awards.domain

/** How many movies each member may nominate per category (web: `NOMINATIONS_PER_AWARD`). */
const val NOMINATIONS_PER_MEMBER = 2

/** Longest category title the server accepts (web: `AWARD_TITLE_MAX_LENGTH`). */
const val AWARD_TITLE_MAX_LENGTH = 60

/** A nominee's result in a category (web: `ScoredNomination`). */
data class ScoredNomination(
    val nomination: Nomination,
    /** Sum of every voter's rank for this nominee; lower is better. */
    val score: Int,
    val winner: Boolean,
    /** Competition ranking by score: ties share a place and the next is skipped (1, 1, 3). */
    val place: Int,
)

/** A category's results: everyone who voted in it, and the nominees best first. */
data class ScoredAward(
    val voters: List<String>,
    val results: List<ScoredNomination>,
)

/**
 * Ranks a category's nominees by total rank across everyone who voted in it
 * (web: `scoreAward`).
 *
 * A nominee a voter left unranked counts as their last place, so it cannot win
 * on an empty column. Ties share the win. A winner is only declared once
 * someone has voted, or when there is a single nominee.
 */
fun scoreAward(nominations: List<Nomination>): ScoredAward {
    val voters = nominations.flatMap { it.ranking.keys }.distinct()
    val lastPlace = nominations.size

    val scored = nominations
        .map { nomination -> nomination to voters.sumOf { nomination.ranking[it] ?: lastPlace } }
        .sortedBy { it.second }

    val best = scored.firstOrNull()?.second
    val decided = voters.isNotEmpty() || nominations.size == 1

    return ScoredAward(
        voters = voters,
        results = scored.map { (nomination, score) ->
            ScoredNomination(
                nomination = nomination,
                score = score,
                winner = decided && score == best,
                place = scored.indexOfFirst { it.second == score } + 1,
            )
        },
    )
}

/** A category with a single nominee has nothing to rank: it wins by default. */
fun AwardCategory.needsRanking(): Boolean = nominations.size > 1

/** Whether [voterId] has ranked every nominee here (web: `hasRankedAward`). */
fun AwardCategory.isRankedBy(voterId: String): Boolean =
    nominations.all { it.ranking[voterId] != null }

/** Whether [voterId] has ranked every category that needs ranking. */
fun AwardsSeason.hasFinishedVoting(voterId: String): Boolean =
    categories.filter { it.needsRanking() }.all { it.isRankedBy(voterId) }

/**
 * A member has finished nominating once they have a nominee in every category.
 * Backing a movie someone else already nominated counts, so a member is never
 * stuck on a category they have no fresh pick for.
 */
fun AwardsSeason.hasFinishedNominating(userId: String): Boolean =
    categories.all { category -> category.nominations.any { userId in it.nominatedBy } }

/**
 * The members who still have their part of the current phase to do. Only
 * nominations and voting ask something of every member; the other phases are
 * run by the club as a whole.
 */
fun AwardsSeason.membersYetToFinish(memberIds: Collection<String>): List<String> = when (step) {
    AwardsStep.NOMINATIONS -> memberIds.filter { !hasFinishedNominating(it) }
    AwardsStep.RANKINGS -> memberIds.filter { !hasFinishedVoting(it) }
    AwardsStep.CATEGORIES, AwardsStep.PRESENTATION, AwardsStep.COMPLETED -> emptyList()
}

/** What members are doing during the phases that ask something of each of them (web: `PHASE_WORK`). */
val AwardsStep.work: String?
    get() = when (this) {
        AwardsStep.NOMINATIONS -> "nominating"
        AwardsStep.RANKINGS -> "voting"
        else -> null
    }

/**
 * Why the season cannot move to [target], or null when it can (web:
 * `stepChangeError`, which the server enforces). The season only ever moves
 * forward, one phase at a time, and the club moves together: nobody can close
 * a phase while a member still has their part of it to do.
 */
fun AwardsSeason.stepChangeError(target: AwardsStep, memberIds: Collection<String>): String? {
    if (target.value != step.value + 1) {
        return "Awards only move forward, one step at a time"
    }
    if (step == AwardsStep.CATEGORIES && categories.isEmpty()) {
        return "Add at least one category before opening nominations"
    }
    val waiting = membersYetToFinish(memberIds).size
    if (waiting > 0) {
        return "Everyone has to finish ${step.work ?: "this step"} first ($waiting still to go)"
    }
    return null
}

/**
 * Nominations in [userId]'s ranked order: ranked ones by rank, then unranked
 * ones in their nomination order. Used to seed the ranking editor.
 */
fun AwardCategory.rankingOrderFor(userId: String): List<Nomination> {
    val (ranked, unranked) = nominations.partition { it.ranking[userId] != null }
    return ranked.sortedBy { it.ranking.getValue(userId) } + unranked
}

/** [userId]'s own nominations in this category. */
fun AwardCategory.nominationsBy(userId: String): List<Nomination> =
    nominations.filter { userId in it.nominatedBy }

/** How many more movies [userId] may nominate here. */
fun AwardCategory.remainingNominationsFor(userId: String): Int =
    (NOMINATIONS_PER_MEMBER - nominationsBy(userId).size).coerceAtLeast(0)

/** Whether every category title is in [revealed], i.e. the presentation is over. */
fun AwardsSeason.allRevealed(revealed: Set<String>): Boolean =
    categories.all { it.title in revealed }

/** Whether two category titles name the same category (web: `isSameCategoryTitle`). */
fun isSameCategoryTitle(a: String, b: String): Boolean = a.trim().lowercase() == b.trim().lowercase()

/**
 * Validates a new category title the way the web does (`awardTitleSchema`
 * plus the uniqueness check). Returns the user-facing error, or null.
 */
fun AwardsSeason.categoryTitleError(title: String, except: String? = null): String? {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return "Give the category a name"
    if (trimmed.length > AWARD_TITLE_MAX_LENGTH) return "Keep it under $AWARD_TITLE_MAX_LENGTH characters"
    if (categories.any { it.title != except && isSameCategoryTitle(it.title, trimmed) }) {
        return "\"$trimmed\" is already a category"
    }
    return null
}
