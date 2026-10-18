package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.LastClubStore
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemberViewModel(
    private val memberRepository: MemberRepository,
    private val lastClubStore: LastClubStore,
    private val clubRepository: ClubRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MemberState())
    val state = _state.asStateFlow()

    init {
        loadClubs(showLoading = true)
        loadMe()
    }

    /**
     * Reloads clubs (and the signed-in member) in place. Existing data stays on screen
     * while refreshing, so the club navigation host isn't torn down.
     */
    fun refresh() {
        val hasData = _state.value.clubs is AsyncResult.Success
        loadClubs(showLoading = !hasData, refresh = true)
        loadMe(refresh = true)
        _state.value.lastClubSlug?.let { loadMembers(it, refresh = true) }
    }

    /** Loads the members of [slug] for the club header (avatar stack and member count). */
    fun loadMembers(slug: String, refresh: Boolean = false) {
        viewModelScope.launch {
            if (slug !in _state.value.members) {
                clubRepository.savedMembers(slug)?.let { saved ->
                    _state.update { if (slug in it.members) it else it.copy(members = it.members + (slug to saved)) }
                }
            }
            clubRepository.getMembers(slug, refresh).onSuccess { members ->
                _state.update { it.copy(members = it.members + (slug to members)) }
            }
        }
    }

    /** Remembers the open club so the next launch starts there. */
    fun rememberClub(slug: String) {
        _state.update { it.copy(lastClubSlug = slug) }
        viewModelScope.launch { lastClubStore.set(slug) }
    }

    private fun loadMe(refresh: Boolean = false) {
        viewModelScope.launch {
            if (_state.value.me == null) {
                memberRepository.savedMember()?.let { saved -> _state.update { it.copy(me = it.me ?: saved) } }
            }
            memberRepository.getMember(refresh).onSuccess { me -> _state.update { it.copy(me = me) } }
        }
    }

    private fun loadClubs(showLoading: Boolean, refresh: Boolean = false) {
        viewModelScope.launch {
            val lastSlug = _state.value.lastClubSlug ?: lastClubStore.get()
            if (showLoading) {
                // Paint the clubs kept from last time at once (offline too), then revalidate.
                val saved = memberRepository.savedClubs()
                _state.update {
                    if (saved != null) it.copy(clubs = AsyncResult.Success(saved), lastClubSlug = lastSlug)
                    else it.copy(clubs = AsyncResult.Loading)
                }
            }
            when (val result = memberRepository.getClubs(refresh)) {
                is Result.Success -> {
                    _state.update { it.copy(clubs = AsyncResult.Success(result.data), lastClubSlug = lastSlug) }
                }
                is Result.Error -> {
                    if (_state.value.clubs !is AsyncResult.Success) {
                        _state.update { it.copy(clubs = AsyncResult.Error(dataError = result.error)) }
                    }
                }
            }
        }
    }
}

data class MemberState(
    val clubs: AsyncResult<List<Club>> = AsyncResult.Loading,
    val me: Member? = null,
    /** The club opened most recently (possibly in an earlier session). */
    val lastClubSlug: String? = null,
    /** Members per club slug, loaded as clubs are opened. */
    val members: Map<String, List<Member>> = emptyMap(),
)
