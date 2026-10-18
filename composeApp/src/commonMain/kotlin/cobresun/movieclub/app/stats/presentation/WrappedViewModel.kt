package cobresun.movieclub.app.stats.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.stats.domain.WrappedCard
import cobresun.movieclub.app.stats.domain.buildWrappedDeck
import cobresun.movieclub.app.stats.domain.currentWrappedYear
import cobresun.movieclub.app.stats.domain.isYearReleased
import cobresun.movieclub.app.stats.domain.scoredReviews
import cobresun.movieclub.app.stats.domain.wrappedYears
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Wrapped screen shows for the selected year. */
sealed interface WrappedContent {
    /** The club hasn't scored anything at all. */
    data object NothingYet : WrappedContent

    /** [year] is this year (or later): its Wrapped arrives on January 1 of the next. */
    data class NotOutYet(val year: Int) : WrappedContent

    /** A released year the club scored nothing in. */
    data class EmptyYear(val year: Int) : WrappedContent

    data class Deck(val year: Int, val cards: List<WrappedCard>) : WrappedContent
}

data class WrappedState(
    val clubSlug: String,
    val clubName: String = "",
    val clubType: ClubType = ClubType.MOVIE,
    /** Released years, newest first. */
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val content: AsyncResult<WrappedContent> = AsyncResult.Loading,
)

sealed interface WrappedAction {
    data class SelectYear(val year: Int) : WrappedAction
    data object Retry : WrappedAction
}

private data class ClubContext(val name: String, val type: ClubType)

private data class Loaded(val stats: StatsData, val club: ClubContext)

/**
 * Club Wrapped for one club. Reads the same reviews and members as the
 * statistics screen and builds the selected year's deck from them.
 */
class WrappedViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val clubRepository: ClubRepository,
    private val memberRepository: MemberRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    /** 0 until the user picks one: the route's year, or else the latest released. */
    private val requestedYear = savedStateHandle.getStateFlow(KEY_YEAR, savedStateHandle.get<Int>("year") ?: 0)

    private val loaded = MutableStateFlow<AsyncResult<Loaded>>(AsyncResult.Loading)

    val state: StateFlow<WrappedState> = combine(loaded, requestedYear) { data, requested ->
        when (data) {
            is AsyncResult.Loading -> WrappedState(clubSlug)
            is AsyncResult.Error -> WrappedState(clubSlug, content = data)
            is AsyncResult.Success -> buildState(data.data, requested)
        }
    }
        // Building a year's deck scans every review (and members³ pairings): off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WrappedState(clubSlug))

    init {
        load()
    }

    fun onAction(action: WrappedAction) {
        when (action) {
            is WrappedAction.SelectYear -> savedStateHandle[KEY_YEAR] = action.year
            WrappedAction.Retry -> load()
        }
    }

    private fun buildState(data: Loaded, requested: Int): WrappedState {
        val reviews = data.stats.reviews
        val years = wrappedYears(reviews)
        val selected = requested.takeIf { it != 0 } ?: years.firstOrNull()
        val content = when {
            reviews.isEmpty() -> WrappedContent.NothingYet
            // Asked for this year, or the club's only reviews are from it.
            selected == null || !isYearReleased(selected) -> WrappedContent.NotOutYet(selected ?: currentWrappedYear())
            else -> {
                val cards = buildWrappedDeck(selected, data.club.type, reviews, data.stats.members)
                if (cards.isEmpty()) WrappedContent.EmptyYear(selected) else WrappedContent.Deck(selected, cards)
            }
        }
        return WrappedState(
            clubSlug = clubSlug,
            clubName = data.club.name,
            clubType = data.club.type,
            years = years,
            selectedYear = selected,
            content = AsyncResult.Success(content),
        )
    }

    private fun load() {
        viewModelScope.launch {
            loaded.value = AsyncResult.Loading
            val reviews = async { reviewsRepository.getReviews(clubSlug) }
            val members = async { clubRepository.getMembers(clubSlug) }
            val clubs = async { memberRepository.getClubs() }
            val r = reviews.await()
            val m = members.await()
            val c = clubs.await()
            val club = (c as? Result.Success)?.data?.firstOrNull { it.slug == clubSlug }
            loaded.value = when {
                r is Result.Success && m is Result.Success && club != null ->
                    AsyncResult.Success(Loaded(StatsData(scoredReviews(r.data), m.data), ClubContext(club.name, club.type)))
                r is Result.Error -> AsyncResult.Error(dataError = r.error)
                m is Result.Error -> AsyncResult.Error(dataError = m.error)
                c is Result.Error -> AsyncResult.Error(dataError = c.error)
                else -> AsyncResult.Error()
            }
        }
    }

    private companion object {
        const val KEY_YEAR = "wrapped_year"
    }
}
