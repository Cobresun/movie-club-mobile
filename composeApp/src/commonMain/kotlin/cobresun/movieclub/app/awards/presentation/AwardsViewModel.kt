package cobresun.movieclub.app.awards.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsRepository
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.Nomination
import cobresun.movieclub.app.awards.domain.NominationCandidate
import cobresun.movieclub.app.awards.domain.allRevealed
import cobresun.movieclub.app.awards.domain.categoryTitleError
import cobresun.movieclub.app.awards.domain.rankingOrderFor
import cobresun.movieclub.app.awards.domain.remainingNominationsFor
import cobresun.movieclub.app.awards.domain.membersYetToFinish
import cobresun.movieclub.app.awards.domain.stepChangeError
import cobresun.movieclub.app.awards.domain.work
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.presentation.UiMessage
import cobresun.movieclub.app.core.presentation.isRetryable
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class AwardsViewModel(
    savedStateHandle: SavedStateHandle,
    private val awardsRepository: AwardsRepository,
    private val reviewsRepository: ReviewsRepository,
    private val clubRepository: ClubRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    private val _state = MutableStateFlow(AwardsState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<AwardsEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private var contextLoaded = false

    init {
        refreshAvailability()
    }

    /**
     * Re-checks whether the club shows Awards (the setting can change in Club
     * settings) and loads the feature the first time it becomes available.
     */
    fun refreshAvailability() {
        viewModelScope.launch {
            val club = (memberRepository.getClubs() as? Result.Success)?.data?.firstOrNull { it.slug == clubSlug }
            val features = clubRepository.getFeatures(clubSlug)
            if (club == null || features !is Result.Success) return@launch
            val available = club.type == ClubType.MOVIE && features.data.awards
            _state.update { it.copy(clubName = club.name, isAvailable = available) }
            if (available && !contextLoaded) {
                contextLoaded = true
                loadContext()
                loadYears()
            }
        }
    }

    fun onAction(action: AwardsAction) {
        when (action) {
            AwardsAction.Refresh -> {
                loadYears(quiet = true)
                loadReviewed()
                viewModelScope.launch {
                    clubRepository.getMembers(clubSlug).onSuccess { m -> _state.update { it.copy(members = m) } }
                }
            }
            is AwardsAction.SelectYear -> if (action.year != _state.value.selectedYear) {
                _state.update {
                    it.copy(selectedYear = action.year, season = AsyncResult.Loading, rankingDrafts = emptyMap(), revealed = emptySet())
                }
                loadSeason()
            }
            AwardsAction.AdvanceStep -> advance()
            AwardsAction.OpenNewAwards -> openNewAwards()
            AwardsAction.DismissNewAwards -> _state.update { it.copy(newAwards = null) }
            is AwardsAction.CreateYear -> createYear(action.year, action.categories)
            AwardsAction.DeleteYear -> deleteYear()
            is AwardsAction.AddCategory -> addCategory(action.title)
            is AwardsAction.RenameCategory -> renameCategory(action.from, action.to)
            is AwardsAction.DeleteCategory -> deleteCategory(action.title)
            is AwardsAction.MoveCategory -> moveCategory(action.from, action.to)
            is AwardsAction.Nominate -> nominate(action.category, action.movieId)
            is AwardsAction.Withdraw -> withdraw(action.category, action.movieId)
            is AwardsAction.MoveNominee -> moveNominee(action.category, action.from, action.to)
            is AwardsAction.SubmitRanking -> submitRanking(action.category)
            is AwardsAction.Reveal -> reveal(action.category)
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            authRepository.getUser().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
        }
        viewModelScope.launch {
            clubRepository.getMembers(clubSlug).onSuccess { m -> _state.update { it.copy(members = m) } }
        }
        loadReviewed()
    }

    private fun loadReviewed() {
        viewModelScope.launch {
            reviewsRepository.getReviews(clubSlug).onSuccess { reviews ->
                val movies = reviews.mapNotNull { review ->
                    val work = review.work
                    val id = work.externalId?.toIntOrNull() ?: return@mapNotNull null
                    ReviewedMovie(
                        candidate = NominationCandidate(id, work.title, work.imageUrl, work.year),
                        reviewedYear = work.addedYearUtc,
                    )
                }
                _state.update { it.copy(reviewed = movies) }
            }
        }
    }

    private fun loadYears(quiet: Boolean = false) {
        viewModelScope.launch {
            if (quiet) _state.update { it.copy(isRefreshing = true) }
            when (val result = awardsRepository.getYears(clubSlug)) {
                is Result.Success -> {
                    val years = result.data
                    val selected = _state.value.selectedYear?.takeIf { it in years } ?: years.firstOrNull()
                    val changed = selected != _state.value.selectedYear
                    _state.update {
                        it.copy(
                            years = AsyncResult.Success(years),
                            selectedYear = selected,
                            season = if (selected == null) null else if (changed) AsyncResult.Loading else it.season,
                        )
                    }
                    if (selected != null) loadSeason(quiet = !changed)
                }
                is Result.Error -> if (_state.value.years is AsyncResult.Success) {
                    emitError(result.error, "refresh awards") { loadYears(quiet = true) }
                } else {
                    _state.update { it.copy(years = AsyncResult.Error(dataError = result.error)) }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadSeason(quiet: Boolean = false) {
        val year = _state.value.selectedYear ?: return
        viewModelScope.launch {
            when (val result = awardsRepository.getSeason(clubSlug, year)) {
                is Result.Success -> if (_state.value.selectedYear == year) {
                    _state.update { it.copy(season = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> if (quiet && _state.value.season is AsyncResult.Success) {
                    emitError(result.error, "refresh awards") { loadSeason(quiet = true) }
                } else {
                    _state.update { it.copy(season = AsyncResult.Error(dataError = result.error)) }
                }
            }
        }
    }

    /** Applies [transform] to the loaded season immediately; reverts and reports on failure. */
    private fun mutate(
        action: String,
        transform: (AwardsSeason) -> AwardsSeason,
        success: String? = null,
        call: suspend (year: Int) -> Result<Unit, DataError.Remote>,
    ) {
        val season = _state.value.loadedSeason ?: return
        _state.update { it.copy(season = AsyncResult.Success(transform(season))) }
        viewModelScope.launch {
            call(season.year)
                .onSuccess { success?.let(::message) }
                .onError { error ->
                    _state.update { it.copy(season = AsyncResult.Success(season)) }
                    emitError(error, action)
                }
            loadSeason(quiet = true)
        }
    }

    private fun advance() {
        val s = _state.value
        val season = s.loadedSeason ?: return
        val next = season.step.manualNext ?: return
        if (season.stepChangeError(next, s.memberIds) != null) return
        _state.update { it.copy(isAdvancing = true) }
        viewModelScope.launch {
            awardsRepository.setStep(clubSlug, season.year, next)
                .onSuccess { _state.update { it.copy(season = AsyncResult.Success(season.copy(step = next))) } }
                .onError { emitError(it, season.step.phase.next?.label?.lowercase() ?: "move on") { advance() } }
            _state.update { it.copy(isAdvancing = false) }
            loadSeason(quiet = true)
        }
    }

    private fun openNewAwards() {
        val latest = (_state.value.years as? AsyncResult.Success)?.data?.firstOrNull()
        _state.update { it.copy(newAwards = NewAwardsState(latestYear = latest)) }
        viewModelScope.launch {
            val result = awardsRepository.getAvailableYears(clubSlug)
            _state.update { s ->
                s.copy(newAwards = s.newAwards?.copy(
                    availableYears = when (result) {
                        is Result.Success -> AsyncResult.Success(result.data)
                        is Result.Error -> AsyncResult.Error(dataError = result.error)
                    }
                ))
            }
        }
        if (latest != null) viewModelScope.launch {
            val loaded = _state.value.loadedSeason?.takeIf { it.year == latest }
            val season = loaded ?: (awardsRepository.getSeason(clubSlug, latest) as? Result.Success)?.data
            val titles = season?.categories?.map { it.title }.orEmpty()
            _state.update { s -> s.copy(newAwards = s.newAwards?.copy(previousCategories = titles)) }
        }
    }

    private fun createYear(year: Int, categories: List<String>) {
        val sheet = _state.value.newAwards ?: return
        if (sheet.isCreating) return
        _state.update { it.copy(newAwards = sheet.copy(isCreating = true)) }
        viewModelScope.launch {
            awardsRepository.createYear(clubSlug, year, categories)
                .onSuccess {
                    // Open the new year, like the web does.
                    _state.update {
                        it.copy(
                            newAwards = null,
                            selectedYear = year,
                            season = AsyncResult.Loading,
                            rankingDrafts = emptyMap(),
                            revealed = emptySet(),
                        )
                    }
                    loadYears(quiet = true)
                }
                .onError { error ->
                    _state.update { s -> s.copy(newAwards = s.newAwards?.copy(isCreating = false)) }
                    emitError(error, "start the $year awards") { createYear(year, categories) }
                }
        }
    }

    private fun deleteYear() {
        val year = _state.value.selectedYear ?: return
        if (_state.value.isDeletingYear) return
        _state.update { it.copy(isDeletingYear = true) }
        viewModelScope.launch {
            awardsRepository.deleteYear(clubSlug, year)
                .onSuccess {
                    // Back to the awards list, which lands on the newest remaining year.
                    val remaining = (_state.value.years as? AsyncResult.Success)?.data.orEmpty() - year
                    val next = remaining.firstOrNull()
                    _state.update {
                        it.copy(
                            years = AsyncResult.Success(remaining),
                            selectedYear = next,
                            season = if (next == null) null else AsyncResult.Loading,
                            rankingDrafts = emptyMap(),
                            revealed = emptySet(),
                            isDeletingYear = false,
                        )
                    }
                    message("Deleted the $year awards")
                    loadYears(quiet = true)
                }
                .onError { error ->
                    _state.update { it.copy(isDeletingYear = false) }
                    emitError(error, "delete the $year awards") { deleteYear() }
                }
        }
    }

    private fun addCategory(raw: String) {
        val season = _state.value.loadedSeason ?: return
        val title = raw.trim()
        season.categoryTitleError(title)?.let { message(it); return }
        mutate(
            action = "add $title",
            transform = { it.copy(categories = it.categories + AwardCategory(title, emptyList())) },
        ) { year -> awardsRepository.addCategory(clubSlug, year, title) }
    }

    private fun renameCategory(from: String, raw: String) {
        val season = _state.value.loadedSeason ?: return
        val to = raw.trim()
        if (to == from) return
        season.categoryTitleError(to, except = from)?.let { message(it); return }
        val order = season.categories.map { it.title }
        mutate(
            action = "rename $from",
            transform = { s ->
                s.copy(categories = s.categories.map { if (it.title == from) AwardCategory(to, emptyList()) else it })
            },
        ) { year -> awardsRepository.renameCategory(clubSlug, year, order, from, to) }
    }

    private fun deleteCategory(title: String) {
        mutate(
            action = "delete $title",
            transform = { s -> s.copy(categories = s.categories.filter { it.title != title }) },
            success = "Deleted $title",
        ) { year -> awardsRepository.deleteCategory(clubSlug, year, title) }
    }

    private fun moveCategory(from: Int, to: Int) {
        val season = _state.value.loadedSeason ?: return
        if (from == to || from !in season.categories.indices || to !in season.categories.indices) return
        val reordered = season.categories.toMutableList().apply { add(to, removeAt(from)) }
        mutate(
            action = "reorder categories",
            transform = { it.copy(categories = reordered) },
        ) { year -> awardsRepository.reorderCategories(clubSlug, year, reordered.map { it.title }) }
    }

    private fun nominate(categoryTitle: String, movieId: Int) {
        val s = _state.value
        val userId = s.currentUserId ?: run { message("Still loading your profile. Try again in a moment."); return }
        val category = s.loadedSeason?.categories?.firstOrNull { it.title == categoryTitle } ?: return
        if (category.remainingNominationsFor(userId) == 0) {
            message("You've used your nominations for $categoryTitle.")
            return
        }
        val candidate = s.reviewed.firstOrNull { it.candidate.movieId == movieId }?.candidate ?: return
        mutate(
            action = "nominate ${candidate.title}",
            transform = { season ->
                season.copy(categories = season.categories.map { c ->
                    if (c.title != categoryTitle) return@map c
                    val existing = c.nominations.firstOrNull { it.movieId == movieId }
                    c.copy(
                        nominations = if (existing != null) {
                            c.nominations.map { if (it.movieId == movieId) it.copy(nominatedBy = it.nominatedBy + userId) else it }
                        } else {
                            c.nominations + Nomination(movieId, candidate.title, candidate.posterUrl, candidate.releaseYear, listOf(userId), emptyMap())
                        }
                    )
                })
            },
        ) { year -> awardsRepository.nominate(clubSlug, year, categoryTitle, movieId, userId) }
    }

    private fun withdraw(categoryTitle: String, movieId: Int) {
        val userId = _state.value.currentUserId ?: return
        mutate(
            action = "remove your nomination",
            transform = { season ->
                season.copy(categories = season.categories.map { c ->
                    if (c.title != categoryTitle) c
                    else c.copy(
                        nominations = c.nominations
                            .map { if (it.movieId == movieId) it.copy(nominatedBy = it.nominatedBy - userId) else it }
                            .filter { it.nominatedBy.isNotEmpty() }
                    )
                })
            },
        ) { year -> awardsRepository.withdrawNomination(clubSlug, year, categoryTitle, movieId, userId) }
    }

    private fun moveNominee(categoryTitle: String, from: Int, to: Int) {
        val s = _state.value
        val category = s.loadedSeason?.categories?.firstOrNull { it.title == categoryTitle } ?: return
        val order = s.rankingFor(category).map { it.movieId }.toMutableList()
        if (from !in order.indices || to !in order.indices || from == to) return
        order.add(to, order.removeAt(from))
        _state.update { it.copy(rankingDrafts = it.rankingDrafts + (categoryTitle to order)) }
    }

    private fun submitRanking(categoryTitle: String) {
        val s = _state.value
        val userId = s.currentUserId ?: return
        val season = s.loadedSeason ?: return
        val category = season.categories.firstOrNull { it.title == categoryTitle } ?: return
        val order = s.rankingFor(category).map { it.movieId }
        _state.update { it.copy(submittingRankings = it.submittingRankings + categoryTitle) }
        viewModelScope.launch {
            awardsRepository.submitRanking(clubSlug, season.year, categoryTitle, order, userId)
                .onSuccess {
                    _state.update { st ->
                        val ranked = st.loadedSeason?.let { current ->
                            current.copy(categories = current.categories.map { c ->
                                if (c.title != categoryTitle) c
                                else c.copy(nominations = c.nominations.map { n ->
                                    n.copy(ranking = n.ranking + (userId to order.indexOf(n.movieId) + 1))
                                })
                            })
                        }
                        st.copy(
                            season = ranked?.let { AsyncResult.Success(it) } ?: st.season,
                            rankingDrafts = st.rankingDrafts - categoryTitle,
                        )
                    }
                    message("Saved your $categoryTitle ranking")
                }
                .onError { emitError(it, "save your $categoryTitle ranking") { submitRanking(categoryTitle) } }
            _state.update { it.copy(submittingRankings = it.submittingRankings - categoryTitle) }
            loadSeason(quiet = true)
        }
    }

    private fun reveal(categoryTitle: String) {
        _state.update { it.copy(revealed = it.revealed + categoryTitle) }
        val s = _state.value
        val season = s.loadedSeason ?: return
        if (season.step == AwardsStep.PRESENTATION && season.allRevealed(s.revealed)) {
            // Same as the web: once every category is revealed, the season is complete.
            viewModelScope.launch {
                awardsRepository.setStep(clubSlug, season.year, AwardsStep.COMPLETED)
                    .onSuccess {
                        _state.update { st ->
                            st.copy(season = st.loadedSeason?.let { AsyncResult.Success(it.copy(step = AwardsStep.COMPLETED)) } ?: st.season)
                        }
                    }
                    .onError { emitError(it, "finish the presentation") }
            }
        }
    }

    private fun message(text: String) {
        _events.tryEmit(AwardsEvent.Message(UiMessage(text)))
    }

    private fun emitError(error: DataError.Remote, action: String, retry: (() -> Unit)? = null) {
        val canRetry = retry != null && error.isRetryable
        _events.tryEmit(
            AwardsEvent.Message(
                UiMessage(
                    text = error.toUserMessage(action),
                    actionLabel = if (canRetry) "Retry" else null,
                    onAction = if (canRetry) retry else null,
                )
            )
        )
    }
}
