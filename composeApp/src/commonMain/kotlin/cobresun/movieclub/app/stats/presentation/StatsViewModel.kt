package cobresun.movieclub.app.stats.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.stats.domain.CreditedPerson
import cobresun.movieclub.app.stats.domain.StatsRepository
import cobresun.movieclub.app.stats.domain.scoredReviews
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the statistics widgets are computed from. */
data class StatsData(
    /** Only works with at least one score. */
    val reviews: List<Review>,
    val members: List<Member>,
)

data class StatsState(
    val clubType: ClubType = ClubType.MOVIE,
    val clubName: String = "",
    val data: AsyncResult<StatsData> = AsyncResult.Loading,
    val isRefreshing: Boolean = false,
    val currentUserId: String? = null,
    /** Cast by external id for the actor leaderboard; null while loading or unavailable. */
    val cast: Map<String, List<CreditedPerson>>? = null,
)

sealed interface StatsAction {
    data object Refresh : StatsAction
    /** The tab became visible: refresh quietly so new scores show up. */
    data object Shown : StatsAction
}

class StatsViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val clubRepository: ClubRepository,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    private var started = false

    fun onAction(action: StatsAction) {
        when (action) {
            StatsAction.Refresh -> {
                // The club context (name/type) may have failed alongside the stats; retry it too.
                if (_state.value.clubName.isBlank()) loadContext()
                load(initial = _state.value.data !is AsyncResult.Success)
            }
            StatsAction.Shown -> if (!started) {
                started = true
                loadContext()
                load(initial = true)
            } else if (_state.value.data is AsyncResult.Success) {
                load(initial = false, showIndicator = false)
            }
        }
    }

    private fun loadContext() {
        viewModelScope.launch {
            memberRepository.getClubs().onSuccess { clubs ->
                clubs.firstOrNull { it.slug == clubSlug }?.let { club ->
                    _state.update { it.copy(clubType = club.type, clubName = club.name) }
                }
            }
        }
        viewModelScope.launch {
            authRepository.getUser().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
        }
        viewModelScope.launch {
            // Only movies have cast; book clubs get an empty map back cheaply.
            statsRepository.getReviewedCast(clubSlug).onSuccess { cast -> _state.update { it.copy(cast = cast) } }
        }
    }

    private fun load(initial: Boolean, showIndicator: Boolean = true) {
        viewModelScope.launch {
            if (initial) _state.update { it.copy(data = AsyncResult.Loading) }
            else if (showIndicator) _state.update { it.copy(isRefreshing = true) }
            val reviews = async { reviewsRepository.getReviews(clubSlug) }
            val members = async { clubRepository.getMembers(clubSlug) }
            val r = reviews.await()
            val m = members.await()
            _state.update { s ->
                when {
                    r is Result.Success && m is Result.Success ->
                        s.copy(data = AsyncResult.Success(StatsData(scoredReviews(r.data), m.data)))
                    s.data is AsyncResult.Success -> s // keep showing what we have
                    else -> s.copy(data = AsyncResult.Error(dataError = (r as? Result.Error)?.error ?: (m as Result.Error).error))
                }.copy(isRefreshing = false)
            }
        }
    }
}
