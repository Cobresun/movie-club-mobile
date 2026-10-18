package cobresun.movieclub.app.awards.presentation

import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.NominationCandidate
import cobresun.movieclub.app.awards.domain.rankingOrderFor
import cobresun.movieclub.app.awards.domain.membersYetToFinish
import cobresun.movieclub.app.awards.domain.stepChangeError
import cobresun.movieclub.app.awards.domain.work
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.member.domain.Member

/* What the Awards tab shows (AwardsState), what it can do (AwardsAction) and its one-off events. */

data class AwardsState(
    val clubName: String = "",
    /** Movie club with the awards setting on; drives the bottom-bar tab. */
    val isAvailable: Boolean = false,
    val years: AsyncResult<List<Int>> = AsyncResult.Loading,
    val selectedYear: Int? = null,
    val season: AsyncResult<AwardsSeason>? = null,
    val isRefreshing: Boolean = false,
    val members: List<Member> = emptyList(),
    val currentUserId: String? = null,
    /** Reviewed movies, used to nominate (filtered to the season's year). */
    val reviewed: List<ReviewedMovie> = emptyList(),
    /** Unsaved ranking edits by category title, best first. */
    val rankingDrafts: Map<String, List<Int>> = emptyMap(),
    val submittingRankings: Set<String> = emptySet(),
    /** Categories revealed during the presentation on this device. */
    val revealed: Set<String> = emptySet(),
    val isAdvancing: Boolean = false,
    val isDeletingYear: Boolean = false,
    /** The "New awards" sheet, while open. */
    val newAwards: NewAwardsState? = null,
) {
    val loadedSeason: AwardsSeason? get() = (season as? AsyncResult.Success)?.data
    val memberIds: List<String> get() = members.map { it.id }

    /** Why the season can't move on yet (web `PhaseControls`), or null when it can. */
    val advanceBlocked: String?
        get() {
            val season = loadedSeason ?: return null
            val next = season.step.manualNext ?: return null
            return season.stepChangeError(next, memberIds)
        }

    /** Members who still have their part of the current phase to do. */
    val waitingOn: List<Member>
        get() {
            val outstanding = loadedSeason?.membersYetToFinish(memberIds) ?: return emptyList()
            return members.filter { it.id in outstanding }
        }

    /** "N categories" or "X of Y members have finished …" (web `PhaseControls`). */
    val phaseProgress: String?
        get() {
            val season = loadedSeason ?: return null
            if (season.step == AwardsStep.CATEGORIES) {
                val count = season.categories.size
                return "$count ${if (count == 1) "category" else "categories"}"
            }
            val work = season.step.work ?: return null
            val finished = members.size - waitingOn.size
            return "$finished of ${members.size} members have finished $work"
        }

    fun member(id: String): Member? = members.firstOrNull { it.id == id }

    /** The ranking shown for a category: the draft if edited, else the user's saved order. */
    fun rankingFor(category: AwardCategory): List<Nomination> {
        val draft = rankingDrafts[category.title]
        val base = currentUserId?.let { category.rankingOrderFor(it) } ?: category.nominations
        if (draft == null) return base
        val byId = base.associateBy { it.movieId }
        return draft.mapNotNull { byId[it] } + base.filter { it.movieId !in draft }
    }

    /** Reviewed movies from the season's year that [category] could still take from this user. */
    fun candidatesFor(category: AwardCategory): List<NominationCandidate> {
        val year = loadedSeason?.year ?: return emptyList()
        val mine = category.nominations.filter { currentUserId in it.nominatedBy }.map { it.movieId }.toSet()
        return reviewed
            .filter { it.reviewedYear == year && it.candidate.movieId !in mine }
            .map { it.candidate }
    }

    fun isRevealed(category: AwardCategory): Boolean =
        loadedSeason?.step == AwardsStep.COMPLETED || category.title in revealed
}

/** What the "New awards" sheet offers (web `NewAwardsModal`). */
data class NewAwardsState(
    /** Years the club reviewed movies in that have no awards yet, newest first. */
    val availableYears: AsyncResult<List<Int>> = AsyncResult.Loading,
    /** The newest existing year, whose categories a new year can copy. */
    val latestYear: Int? = null,
    val previousCategories: List<String> = emptyList(),
    val isCreating: Boolean = false,
)

/** A reviewed movie and the year it was added to reviews (web filters nominations by this). */
data class ReviewedMovie(val candidate: NominationCandidate, val reviewedYear: Int?)

sealed interface AwardsEvent {
    data class Message(val message: UiMessage) : AwardsEvent
}

sealed interface AwardsAction {
    data object Refresh : AwardsAction
    data class SelectYear(val year: Int) : AwardsAction
    data object AdvanceStep : AwardsAction

    data object OpenNewAwards : AwardsAction
    data object DismissNewAwards : AwardsAction
    data class CreateYear(val year: Int, val categories: List<String>) : AwardsAction
    data object DeleteYear : AwardsAction

    data class AddCategory(val title: String) : AwardsAction
    data class RenameCategory(val from: String, val to: String) : AwardsAction
    data class DeleteCategory(val title: String) : AwardsAction
    data class MoveCategory(val from: Int, val to: Int) : AwardsAction

    data class Nominate(val category: String, val movieId: Int) : AwardsAction
    data class Withdraw(val category: String, val movieId: Int) : AwardsAction

    data class MoveNominee(val category: String, val from: Int, val to: Int) : AwardsAction
    data class SubmitRanking(val category: String) : AwardsAction

    data class Reveal(val category: String) : AwardsAction
}
