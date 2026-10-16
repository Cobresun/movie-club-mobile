package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
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
        loadClubs(showLoading = !hasData)
        loadMe()
    }

    private fun loadMe() {
        viewModelScope.launch {
            memberRepository.getMember().onSuccess { me -> _state.update { it.copy(me = me) } }
        }
    }

    private fun loadClubs(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(clubs = AsyncResult.Loading) }
            when (val result = memberRepository.getClubs()) {
                is Result.Success -> {
                    _state.update { it.copy(clubs = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    if (showLoading) _state.update { it.copy(clubs = AsyncResult.Error(dataError = result.error)) }
                }
            }
        }
    }
}

data class MemberState(
    val clubs: AsyncResult<List<Club>> = AsyncResult.Loading,
    val me: Member? = null,
)
